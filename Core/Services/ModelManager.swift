import SwiftUI

class ModelManager: ObservableObject {
    @Published var allModels: [Recognizing] = []
    @Published var activeModel: Recognizing
    
    init() {
        let placeholder = PlaceholderModel()
        self.activeModel = placeholder
        self.allModels = [placeholder]
    }
    
    func loadModelsIfNeeded() {
        if !(activeModel is PlaceholderModel) && !allModels.isEmpty { return }
        
        print("ModelManager: Loading models...")
        
        // В реальном проекте тут могут быть разные классы:
        // let floatModel = MNISTFloat16()
        // let quantModel = MNISTQuantized()
        
        // Пока используем твой MNISTRecognizer (предполагаем, что он есть)
        // Для теста создадим две версии одной модели с разными именами
        let modelV1 = MNISTRecognizer()
        // Хакаем имя для демо (в реальности это должен быть другой класс или конфиг)
        // Если MNISTRecognizer - это класс, лучше создать его наследника или конфигурировать в init
        
        // Пример списка моделей:
        self.allModels = [
            modelV1,
            // Сюда можно добавить Tesseract или Vision, если реализуешь их
            PlaceholderModel(name: "Apple Vision (Demo)", desc: "iOS Native OCR")
        ]
        
        self.activeModel = modelV1
    }
    
    func setActiveModel(_ model: Recognizing) {
        // Тут можно добавить логику выгрузки старой модели из памяти, если нужно
        activeModel = model
        print("Model switched to: \(model.name)")
    }
}

// Обновленная заглушка под новый протокол
class PlaceholderModel: Recognizing {
    var name: String
    var description: String
    
    init(name: String = "Placeholder", desc: String = "Loading...") {
        self.name = name
        self.description = desc
    }
    
    func recognize(cgImage: CGImage) async throws -> [Prediction] {
        // Возвращаем фейковые данные для теста
        return [
            Prediction(label: "Test", confidence: 0.99),
            Prediction(label: "Mock", confidence: 0.50)
        ]
    }
}
