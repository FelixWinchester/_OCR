import SwiftUI

struct MetricsPanel: View {
    let metrics: PerformanceMetrics?
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Статистика")
                .font(.headline)
                .foregroundColor(.primary)
            
            if let metrics = metrics {
                // Секция 1: Топ предсказания
                VStack(alignment: .leading, spacing: 8) {
                    Text("Top 3 Predictions")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)
                    
                    ForEach(metrics.topPredictions) { prediction in
                        HStack {
                            Text(prediction.label)
                                .font(.system(.body, design: .monospaced))
                                .fontWeight(.bold)
                                .frame(width: 30, alignment: .leading)
                            
                            // Бар уверенности
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule().fill(Color.gray.opacity(0.2))
                                    Capsule()
                                        .fill(prediction.confidence > 0.8 ? Color.green : Color.blue)
                                        .frame(width: geo.size.width * prediction.confidence)
                                }
                            }
                            .frame(height: 8)
                            
                            Text("\(Int(prediction.confidence * 100))%")
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .frame(width: 35, alignment: .trailing)
                        }
                    }
                }
                .padding()
                .background(Color.secondary.opacity(0.05))
                .cornerRadius(12)
                
                // Секция 2: Технические метрики (Grid)
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    metricTile(title: "Inference", value: String(format: "%.0f ms", metrics.inferenceTime * 1000), icon: "stopwatch")
                    metricTile(title: "Total Time", value: String(format: "%.0f ms", metrics.totalTime * 1000), icon: "clock")
                    metricTile(title: "Memory", value: metrics.peakMemoryUsage, icon: "memorychip")
                    metricTile(title: "Input Size", value: metrics.inputSize, icon: "arrow.up.left.and.arrow.down.right")
                    metricTile(title: "Thermal", value: metrics.thermalState, icon: "flame", color: metrics.thermalState == "Norm" ? .green : .orange)
                }
                
            } else {
                Text("Нет данных о последнем запуске")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding(.vertical, 20)
            }
        }
        .padding()
        .background(Color(uiColor: .secondarySystemBackground)) // Или .ultraThinMaterial
        .cornerRadius(20)
    }
    
    @ViewBuilder
    private func metricTile(title: String, value: String, icon: String, color: Color = .blue) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)
                .frame(width: 30)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .textCase(.uppercase)
                Text(value)
                    .font(.system(.callout, design: .rounded))
                    .fontWeight(.semibold)
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white) // Или Color(uiColor: .tertiarySystemBackground) для темной темы
        .cornerRadius(10)
        .shadow(color: .black.opacity(0.03), radius: 2, x: 0, y: 1)
    }
}
