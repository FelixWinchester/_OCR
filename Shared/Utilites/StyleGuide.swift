import SwiftUI

public enum AppTheme {
    static let primaryAccent = Color.blue
    static let backgroundColor = Color(.systemGroupedBackground)
    static let secondaryBackground = Color(.secondarySystemGroupedBackground)
    
    // Градиент для кнопок
    static let primaryGradient = LinearGradient(
        colors: [.blue, .blue.opacity(0.8)],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}

struct CardViewModifier: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding()
            .background(BlurView(style: .systemUltraThinMaterial)) // Эффект стекла
            .cornerRadius(20)
            .shadow(color: .black.opacity(0.08), radius: 10, x: 0, y: 5)
    }
}

// Вспомогательный View для блюра
struct BlurView: UIViewRepresentable {
    let style: UIBlurEffect.Style
    func makeUIView(context: Context) -> UIVisualEffectView {
        UIVisualEffectView(effect: UIBlurEffect(style: style))
    }
    func updateUIView(_ uiView: UIVisualEffectView, context: Context) {}
}

extension View {
    func asCard() -> some View {
        self.modifier(CardViewModifier())
    }
}
