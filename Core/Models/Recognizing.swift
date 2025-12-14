import CoreGraphics
import Foundation

/// Структура одного предсказания
struct Prediction: Identifiable, Hashable {
    let id = UUID()
    let label: String
    let confidence: Double
}

protocol Recognizing {
    var name: String { get }
    var description: String { get }
    
    /// Возвращает массив предсказаний (желательно отсортированный по убыванию уверенности)
    func recognize(cgImage: CGImage) async throws -> [Prediction]
}

enum RecognitionError: Error {
    case preprocessingFailed
    case modelNotLoaded
    case recognitionFailed
    case inferenceFailed(String)
    case postprocessingFailed
}
