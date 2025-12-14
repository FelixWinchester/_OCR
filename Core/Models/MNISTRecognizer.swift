import CoreML
import Vision
import CoreGraphics

class MNISTRecognizer: Recognizing {
    let name: String = "MNIST"
    let description: String = "Классификация рукописных цифр (0–9)"
    
    private var model: MNISTClassifier?
    
    init() {
        loadModel()
    }
    
    private func loadModel() {
        // Загрузка модели синхронная (инициализатор)
        // Если здесь возникнет ошибка "Expression is async", значит init тоже стал асинхронным,
        // но обычно prediction — это то, что требует await.
        do {
            let config = MLModelConfiguration()
            model = try MNISTClassifier(configuration: config)
        } catch {
            print("❌ Ошибка загрузки модели MNIST: \(error)")
        }
    }
    
    func recognize(cgImage: CGImage) async throws -> [Prediction] {
        guard let model = model else {
            throw RecognitionError.modelNotLoaded
        }
        
        do {
            let input = try MNISTClassifierInput(imageWith: cgImage)
            
            // ДОБАВЛЕНО await
            let output = try await model.prediction(input: input)
            
            // Преобразуем словарь всех вероятностей
            let results = output.labelProbabilities.map { key, value in
                Prediction(label: String(key), confidence: value)
            }
            
            return results.sorted { $0.confidence > $1.confidence }
            
        } catch {
            // Можно распечатать ошибку для отладки
            print("Ошибка распознавания: \(error)")
            throw RecognitionError.recognitionFailed
        }
    }
}
