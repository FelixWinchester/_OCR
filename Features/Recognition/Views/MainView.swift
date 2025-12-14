import SwiftUI
import UIKit  // Для UIRectCorner и других UIKit-элементов

struct MainView: View {
    // Мы используем StateObject, но инициализируем его вручную в init(),
    // чтобы передать туда все зависимости.
    @StateObject private var viewModel: RecognitionViewModel
    
    // Конструктор MainView собирает все зависимости
    init() {
        // Создаем сервисы
        let modelManager = ModelManager()
        let metricsService = MetricsService() // Предполагаем, что есть init()
        let imageProcessor = ImageProcessor() // Предполагаем, что есть init()
        
        // Инициализируем ViewModel
        // _viewModel — это доступ к обертке StateObject
        _viewModel = StateObject(wrappedValue: RecognitionViewModel(
            modelManager: modelManager,
            metricsService: metricsService,
            imageProcessor: imageProcessor
        ))
    }
    
    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottom) {
                // Фон (убедись, что AppTheme существует, иначе используй Color.white)
                AppTheme.backgroundColor.ignoresSafeArea()
                
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 25) {
                        // СЕКЦИЯ ВЫБОРА ФОТО
                        imageSelectionSection
                            .padding(.top, 10)
                        
                        // КНОПКА ЗАПУСКА
                        if viewModel.selectedImage != nil {
                            PrimaryButton(title: "Распознать", action: {
                                withAnimation(.spring()) {
                                    viewModel.runRecognition()
                                }
                            }, isDisabled: viewModel.isLoading)
                            .transition(.scale.combined(with: .opacity))
                        }
                        
                        Spacer(minLength: 250)
                    }
                    .padding()
                }
                
                // НИЖНЯЯ ПАНЕЛЬ
                infoPanel
            }
            .navigationTitle("OCR")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {  // Убрали id для избежания ошибки с CustomizableToolbarContent
                ToolbarItem(placement: .topBarLeading) {
                    modelMenu
                }
            }
        }
        .onAppear {
            // Теперь безопасно вызываем загрузку
            viewModel.onAppear()
        }
        .sheet(isPresented: $viewModel.showingImagePicker) {
            UnifiedImagePicker(
                selectedImage: $viewModel.selectedImage,
                isPresented: $viewModel.showingImagePicker,  // Поменяли порядок аргументов
                sourceType: viewModel.pickerSourceType
            )
        }
        .alert("Источник", isPresented: $viewModel.showingSourceAlert) {
            Button("Камера") { viewModel.selectImageSource(source: .camera) }
            Button("Галерея") { viewModel.selectImageSource(source: .photoLibrary) }
            Button("Отмена", role: .cancel) {}
        }
    }
    
    // MARK: - Вспомогательные View компоненты
    
    private var modelMenu: some View {
        Menu {
            Picker("Модель", selection: $viewModel.activeModelName) {
                ForEach(viewModel.allModels, id: \.name) { model in
                    Text(model.name).tag(model.name)
                }
            }
        } label: {
            HStack(spacing: 4) {
                Image(systemName: "cpu")
                Text(viewModel.activeModelName)
                    .font(.subheadline)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            // Если Color.blue.opacity работает плохо, используй стандартный цвет
            .background(Capsule().fill(Color.blue.opacity(0.1)))
        }
    }
    
    private var imageSelectionSection: some View {
        Button {
            viewModel.presentImageSourceAlert()
        } label: {
            VStack(spacing: 15) {
                if let image = viewModel.selectedImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .frame(maxHeight: 300)
                        .cornerRadius(15)
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "photo.on.rectangle.angled")
                            .font(.system(size: 40))
                            .foregroundColor(.blue)
                        Text("Нажмите, чтобы выбрать фото")
                            .font(.headline)
                            .foregroundColor(.primary)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 250)
                    .background(
                        RoundedRectangle(cornerRadius: 20)
                            .strokeBorder(style: StrokeStyle(lineWidth: 2, dash: [8]))
                            .foregroundColor(.gray.opacity(0.4))
                    )
                }
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
    
    private var infoPanel: some View {
        VStack(spacing: 15) {
            Capsule()
                .fill(Color.secondary.opacity(0.3))
                .frame(width: 40, height: 4)
                .padding(.top, 8)
            
            if viewModel.isLoading {
                HStack {
                    ProgressView()
                    Text("Обработка нейросетью...")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.bottom, 50)
            } else {
                VStack(spacing: 16) {
                    ResultCard(result: viewModel.recognitionResult)
                    MetricsPanel(metrics: viewModel.metrics)
                }
                .padding(.bottom, 40)
            }
        }
        .padding(.horizontal)
        // Если BlurView у тебя нет, замени на .background(.ultraThinMaterial)
        .background(BlurView(style: .systemThickMaterial))
        // Кастомный cornerRadius из View-Extensions.swift (если ошибки, добавь extension ниже)
        .cornerRadius(30)
        .shadow(color: .black.opacity(0.1), radius: 10)
    }
}
