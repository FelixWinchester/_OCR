import Foundation
import CoreML
import CoreGraphics

/// Асинхронный и безопасный wrapper вокруг сгенерированного MNISTClassifier.
/// Использует асинхронный loader (если доступен) и не блокирует главный поток при инициализации.
class MNISTModelWrapper: Recognizing {
    let name: String = "MNISTClassifier"
    let description: String = "CoreML MNIST (28×28 grayscale) — загружается в фоне."
    
    private let model: MNISTClassifier
    
    private init(model: MNISTClassifier) {
        self.model = model
    }
    
    /// Пытаемся создать обёртку асинхронно. Возвращает nil при ошибке.
    static func createIfPossible() async -> MNISTModelWrapper? {
        // Если доступна async loader (iOS 15+/macOS newer), используем его
        if #available(iOS 15.0, macOS 12.0, *) {
            do {
                let config = MLModelConfiguration()
                // Загрузка модели асинхронна
                let loaded = try await MNISTClassifier.load(configuration: config)
                return MNISTModelWrapper(model: loaded)
            } catch {
                print("MNISTModelWrapper: не удалось асинхронно загрузить модель: \(error)")
                return nil
            }
        } else {
            // fallback — попытка синхронной загрузки в фоновой очереди
            return await withCheckedContinuation { cont in
                DispatchQueue.global(qos: .utility).async {
                    do {
                        let config = MLModelConfiguration()
                        // Синхронная инициализация для старых версий/fallback
                        let loaded = try MNISTClassifier(configuration: config)
                        cont.resume(returning: MNISTModelWrapper(model: loaded))
                    } catch {
                        print("MNISTModelWrapper: не удалось синхронно загрузить модель: \(error)")
                        cont.resume(returning: nil)
                    }
                }
            }
        }
    }
    
    // MARK: - Recognizing Conformance
    
    func recognize(cgImage: CGImage) async throws -> [Prediction] {
        // 1. Создаем input
        let input = try MNISTClassifierInput(imageWith: cgImage)
        
        // 2. Делаем предсказание (ДОБАВЛЕНО await)
        let output = try await model.prediction(input: input)
        
        // 3. Преобразуем результаты
        let predictions = output.labelProbabilities.map { (label, confidence) in
            Prediction(label: String(label), confidence: confidence)
        }
        
        // 4. Сортируем
        return predictions.sorted { $0.confidence > $1.confidence }
    }
}
