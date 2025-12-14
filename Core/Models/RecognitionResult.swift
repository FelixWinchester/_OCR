import Foundation

struct RecognitionResult: Identifiable {
    let id = UUID()
    let text: String
    let confidence: Double
    let modelName: String // Добавим, какая модель это распознала
}
