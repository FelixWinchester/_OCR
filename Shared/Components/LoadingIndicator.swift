import SwiftUI

struct LoadingIndicator: View {
    var body: some View {
        ProgressView()
            .scaleEffect(1.5)
            .padding()
            .background(Color.white.opacity(0.7))
            .cornerRadius(10)
    }
}
