
import SwiftUI
import Foundation
import AVFoundation
import LocalAuthentication
import UIKit
import Combine
import CoreMotion

// MARK: - Alarm Yöneticisi Sınıfı
class BackgroundTheftAlarmManager: ObservableObject {
    @Published var isArmed: Bool = false
    private var audioPlayer: AVAudioPlayer?
    private var backgroundTaskID: UIBackgroundTaskIdentifier = .invalid
    private let motionManager = CMMotionManager()

    init() {
        setupAudioSession()
        registerBackgroundTasks()
    }

    private func setupAudioSession() {
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Ses oturumu kurulamadı: \(error.localizedDescription)")
        }
    }

    private func registerBackgroundTasks() {
        NotificationCenter.default.addObserver(self, selector: #selector(appDidEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
    }

    @objc private func appDidEnterBackground() {
        if isArmed {
            backgroundTaskID = UIApplication.shared.beginBackgroundTask(expirationHandler: {
                UIApplication.shared.endBackgroundTask(self.backgroundTaskID)
                self.backgroundTaskID = .invalid
            })
        }
    }

    // Alarmı Kur ve Sensörü Başlat
    func armAlarm() {
        isArmed = true
        startMotionDetection()
    }

    // Hareket Algılandığında Doğrudan Alarmı Çal
    private func startMotionDetection() {
        if motionManager.isDeviceMotionAvailable {
            motionManager.deviceMotionUpdateInterval = 0.1
            motionManager.startDeviceMotionUpdates(to: OperationQueue.main) { [weak self] motion, error in
                guard let self = self, let motion = motion, self.isArmed else { return }
                
                let rot = motion.rotationRate
                let userAccel = motion.userAcceleration
                
                // Hassasiyet eşikleri (Cihaz oynatıldığı an tetiklenir)
                if abs(rot.x) > 0.15 || abs(rot.y) > 0.15 || abs(rot.z) > 0.15 ||
                   abs(userAccel.x) > 0.1 || abs(userAccel.y) > 0.1 || abs(userAccel.z) > 0.1 {
                    
                    self.triggerAlarm()
                }
            }
        }
    }

    private func stopMotionDetection() {
        if motionManager.isDeviceMotionActive {
            motionManager.stopDeviceMotionUpdates()
        }
    }

    // Alarmı Kapatmak İçin Face ID Şartı (Sadece siz kapatabilirsiniz)
    func disarmAlarmWithFaceID(completion: @escaping (Bool) -> Void) {
        let context = LAContext()
        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "Alarmı kapatmak için yüzünüzü doğrulayın.") { success, error in
            DispatchQueue.main.async {
                if success {
                    // Doğru kişi (Sizsiniz): Alarm kapanır
                    self.isArmed = false
                    self.stopMotionDetection()
                    self.stopAlarmSound()
                    if self.backgroundTaskID != .invalid {
                        UIApplication.shared.endBackgroundTask(self.backgroundTaskID)
                        self.backgroundTaskID = .invalid
                    }
                    completion(true)
                } else {
                    // Yabancı veya başarısız deneme
                    completion(false)
                }
            }
        }
    }

    // Alarmı Tetikle ve Çal
    func triggerAlarm() {
        guard isArmed else { return }
        if audioPlayer?.isPlaying == true { return }
        
        if let soundURL = Bundle.main.url(forResource: "alarm", withExtension: "mp3") {
            do {
                audioPlayer = try AVAudioPlayer(contentsOf: soundURL)
                audioPlayer?.numberOfLoops = -1 // Sınırsız döngü
                audioPlayer?.prepareToPlay()
                audioPlayer?.play()
            } catch {
                print("Alarm sesi çalınamadı: \(error.localizedDescription)")
            }
        }
    }

    private func stopAlarmSound() {
        audioPlayer?.stop()
        audioPlayer = nil
    }
}

// MARK: - SwiftUI Görünümü
struct ContentView: View {
    @StateObject private var alarmManager = BackgroundTheftAlarmManager()

    var body: some View {
        VStack(spacing: 20) {
            Text(alarmManager.isArmed ? "🚨 Güvenlik Aktif (Dokunulması Bekleniyor)" : "🔒 Alarm Kapalı")
                .font(.headline)
                .foregroundColor(alarmManager.isArmed ? .red : .gray)
                .multilineTextAlignment(.center)

            Button(action: {
                if alarmManager.isArmed {
                    // Kapatmaya çalışırken Face ID sorar
                    alarmManager.disarmAlarmWithFaceID { success in
                        if success {
                            print("Alarm başarıyla kapatıldı.")
                        } else {
                            print("Yüz doğrulanamadı!")
                        }
                    }
                } else {
                    alarmManager.armAlarm()
                }
            }) {
                Text(alarmManager.isArmed ? "Alarmı Kapat (Face ID)" : "Korunmayı Başlat")
                    .padding()
                    .background(alarmManager.isArmed ? Color.green : Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            
            Button(action: {
                alarmManager.triggerAlarm()
            }) {
                Text("🔊 Sesi Test Et")
                    .padding()
                    .background(Color.orange)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
        }
        .padding()
    }
}
