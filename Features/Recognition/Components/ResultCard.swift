import SwiftUI

struct ResultCard: View {
    let result: RecognitionResult?
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text("РЕЗУЛЬТАТ")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(.secondary)
                
                if let result = result {
                    Text(result.text)
                        .font(.system(size: 40, weight: .black, design: .rounded))
                        .foregroundColor(.primary)
                } else {
                    Text("---")
                        .font(.system(size: 40, weight: .black, design: .rounded))
                        .foregroundColor(.gray.opacity(0.3))
                }
            }
            
            Spacer()
            
            if let result = result {
                VStack(alignment: .trailing) {
                    Text("\(Int(result.confidence * 100))%")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(confidenceColor(result.confidence))
                    Text("уверенность")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color.primary.opacity(0.03))
        .cornerRadius(15)
    }
    
    private func confidenceColor(_ val: Double) -> Color {
        if val > 0.8 { return .green }
        if val > 0.5 { return .orange }
        return .red
    }
}
