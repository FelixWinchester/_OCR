import SwiftUI
import Combine
import UIKit

// MARK: - RecognitionViewModel

@MainActor
class RecognitionViewModel: ObservableObject {
    
    // MARK: - UI State
    @Published var selectedImage: UIImage?
    @Published var recognitionResult: RecognitionResult?
    @Published var isLoading: Bool = false
    @Published var showingImagePicker = false
    @Published var showingSourceAlert = false
    @Published var pickerSourceType: UIImagePickerController.SourceType = .photoLibrary
    
    // MARK: - Models State
    @Published var activeModel: Recognizing
    @Published var allModels: [Recognizing] = []
    @Published var activeModelName: String // Связано с Picker в MainView
    
    // MARK: - Services
    private let modelManager: ModelManager
    private let metricsService: MetricsService
    private let imageProcessor: ImageProcessor
    
    // MARK: - Metrics
    @Published private(set) var metrics: PerformanceMetrics?
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Initialization
    init(modelManager: ModelManager, metricsService: MetricsService, imageProcessor: ImageProcessor) {
        self.modelManager = modelManager
        self.metricsService = metricsService
        self.imageProcessor = imageProcessor
        
        // Начальное состояние берем из менеджера
        self.activeModel = modelManager.activeModel
        self.allModels = modelManager.allModels
        self.activeModelName = modelManager.activeModel.name
        
        setupBindings()
    }
    
    // MARK: - Lifecycle
    func onAppear() {
        modelManager.loadModelsIfNeeded()
    }
    
    // MARK: - Private Setup
    private func setupBindings() {
        // 1. Синхронизация списка всех моделей
        modelManager.$allModels
            .receive(on: RunLoop.main)
            .assign(to: &$allModels)
            
        // 2. Слушаем изменения активной модели в Менеджере (Logic -> UI)
        modelManager.$activeModel
            .receive(on: RunLoop.main)
            .sink { [weak self] newModel in
                guard let self = self else { return }
                
                // Обновляем ссылку на модель, если она изменилась
                if self.activeModel.name != newModel.name {
                    self.activeModel = newModel
                }
                
                // Важно: обновляем имя для Picker только если оно реально другое
                // Это предотвращает лишние срабатывания подписчика $activeModelName
                if self.activeModelName != newModel.name {
                    self.activeModelName = newModel.name
                }
            }
            .store(in: &cancellables)
        
        // 3. Слушаем изменения имени модели из Picker (UI -> Logic)
        $activeModelName
            .removeDuplicates() // Игнорируем повторные установки одного и того же имени
            .dropFirst()        // Пропускаем установку при инициализации
            .receive(on: RunLoop.main)
            .sink { [weak self] newName in
                guard let self = self else { return }
                
                // Если менеджер уже работает с этой моделью — ничего не делаем (разрыв цикла)
                if self.modelManager.activeModel.name == newName { return }
                
                // Ищем модель в списке и просим менеджер переключиться
                if let model = self.allModels.first(where: { $0.name == newName }) {
                    self.modelManager.setActiveModel(model)
                }
            }
            .store(in: &cancellables)
            
        // 4. Синхронизация метрик
        metricsService.$latestMetrics
            .receive(on: RunLoop.main)
            .assign(to: &$metrics)
    }
    
    // MARK: - Recognition Pipeline
    func runRecognition() {
        guard let image = selectedImage else { return }
        
        isLoading = true
        recognitionResult = nil
        
        Task {
            let startTime = Date()
            
            do {
                // 1. Препроцессинг
                guard let processedImage = imageProcessor.preprocess(image: image) else {
                    throw RecognitionError.preprocessingFailed
                }
                let preprocessingTime = Date().timeIntervalSince(startTime)
                
                // 2. Распознавание (выполнение модели)
                let predictions = try await activeModel.recognize(cgImage: processedImage)
                let inferenceTime = Date().timeIntervalSince(startTime) - preprocessingTime
                
                // Топ-1 результат
                let topPrediction = predictions.first ?? Prediction(label: "?", confidence: 0.0)
                
                // 3. Сбор данных для метрик
                let ramUsage = metricsService.getMemoryUsage()
                let thermal = metricsService.getThermalState()
                let inputSizeStr = "\(processedImage.width)x\(processedImage.height)"
                
                let performanceMetrics = PerformanceMetrics(
                    preprocessingTime: preprocessingTime,
                    inferenceTime: inferenceTime,
                    postprocessingTime: 0.0,
                    inputSize: inputSizeStr,
                    peakMemoryUsage: ramUsage,
                    thermalState: thermal,
                    topPredictions: Array(predictions.prefix(3))
                )
                
                // 4. Формирование результата
                let result = RecognitionResult(
                    text: topPrediction.label,
                    confidence: topPrediction.confidence,
                    modelName: activeModel.name
                )
                
                // Обновление UI
                self.recognitionResult = result
                self.metricsService.recordMetrics(performanceMetrics)
                self.isLoading = false
                
            } catch {
                print("❌ Ошибка распознавания: \(error)")
                self.isLoading = false
                // Здесь можно добавить @Published var errorMessage для уведомления пользователя
            }
        }
    }
    
    // MARK: - UI Actions
    func selectImageSource(source: UIImagePickerController.SourceType) {
        pickerSourceType = source
        showingSourceAlert = false
        showingImagePicker = true
    }
    
    func presentImageSourceAlert() {
        showingSourceAlert = true
    }
}
