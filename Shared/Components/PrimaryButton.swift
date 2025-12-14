import SwiftUI

struct PrimaryButton: View {
    let title: String
    let action: () -> Void
    var isDisabled: Bool = false
    
    var body: some View {
        Button(action: {
            let impact = UIImpactFeedbackGenerator(style: .medium)
            impact.impactOccurred()
            action()
        }) {
            Text(title)
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 16)
                .background(isDisabled ? AnyView(Color.gray) : AnyView(AppTheme.primaryGradient))
                .cornerRadius(15)
                .shadow(color: (isDisabled ? Color.clear : Color.blue.opacity(0.3)), radius: 8, y: 4)
        }
        .disabled(isDisabled)
        .scaleEffect(isDisabled ? 0.98 : 1.0)
        .animation(.spring(), value: isDisabled)
    }
}
