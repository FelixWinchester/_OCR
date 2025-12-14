import Foundation
import UIKit

struct PerformanceMetrics {
    // Тайминги
    let preprocessingTime: TimeInterval
    let inferenceTime: TimeInterval
    let postprocessingTime: TimeInterval
    
    // Новые полезные метрики
    let inputSize: String         // Например "28x28"
    let peakMemoryUsage: String   // Например "14.5 MB"
    let thermalState: String      // Например "Nominal" или "Fair"
    let topPredictions: [Prediction] // Топ-3 результата для детального показа
    
    var totalTime: TimeInterval {
        preprocessingTime + inferenceTime + postprocessingTime
    }
}
