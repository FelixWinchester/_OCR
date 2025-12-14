import UIKit

class ImageProcessor {
    
    func preprocess(image: UIImage) -> CGImage? {
        // Базовая предобработка - ресайз до 28x28 для MNIST
        let targetSize = CGSize(width: 28, height: 28)
        
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1.0
        
        let renderer = UIGraphicsImageRenderer(size: targetSize, format: format)
        let resizedImage = renderer.image { context in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
        
        return resizedImage.cgImage
    }
}
