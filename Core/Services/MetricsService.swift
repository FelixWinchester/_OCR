import Foundation
import Combine
import UIKit

class MetricsService: ObservableObject {
    @Published private(set) var latestMetrics: PerformanceMetrics?
    @Published private(set) var metricsHistory: [PerformanceMetrics] = []
    
    func recordMetrics(_ metrics: PerformanceMetrics) {
        latestMetrics = metrics
        metricsHistory.append(metrics)
        print("📈 Stats: \(metrics.totalTime * 1000)ms | RAM: \(metrics.peakMemoryUsage)")
    }
    
    func clearHistory() {
        metricsHistory.removeAll()
        latestMetrics = nil
    }
    
    // MARK: - System Helpers
    
    /// Получает текущее использование памяти (RAM)
    func getMemoryUsage() -> String {
        var taskInfo = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &taskInfo) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &count)
            }
        }
        
        if kerr == KERN_SUCCESS {
            let usedMB = Double(taskInfo.resident_size) / 1024.0 / 1024.0
            return String(format: "%.1f MB", usedMB)
        } else {
            return "N/A"
        }
    }
    
    /// Получает тепловое состояние устройства
    func getThermalState() -> String {
        switch ProcessInfo.processInfo.thermalState {
        case .nominal: return "Norm"
        case .fair: return "Warm"
        case .serious: return "Hot"
        case .critical: return "Crit"
        @unknown default: return "?"
        }
    }
}
