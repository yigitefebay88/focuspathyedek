//
// ContentView.swift
// focuspathios
//
// Created by Yiğit Bay on 10.10.2026.
//

import SwiftUI
import AVFoundation
import Combine
import UserNotifications

struct ContentView: View {
    @State private var selectedTab: BottomTab = .anaSayfa
    @State private var showSettings = false
    @State private var showAuth = false
    @AppStorage("isLoggedIn") private var isLoggedIn = false
    @AppStorage("userEmail") private var userEmail = ""
    @State private var showIntro = true
    
    var body: some View {
        ZStack {
            if showIntro {
                IntroView {
                    withAnimation(.easeInOut(duration: 0.6)) {
                        showIntro = false
                    }
                }
                .transition(.opacity)
            } else {
                ZStack(alignment: .bottom) {
                    Group {
                        switch selectedTab {
                        case .anaSayfa:
                            AnaSayfaView(showSettings: $showSettings, showAuth: $showAuth, isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                        case .gorevler:
                            GorevlerView(showSettings: $showSettings, showAuth: $showAuth, isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                        case .oyunlar:
                            OyunlarView(showSettings: $showSettings, showAuth: $showAuth, isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                        case .takvim:
                            TakvimView(showSettings: $showSettings, showAuth: $showAuth, isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                        case .ofis:
                            OfisView(showSettings: $showSettings, showAuth: $showAuth, isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                        case .su:
                            SuView(showSettings: $showSettings, showAuth: $showAuth, isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .gesture(
                        DragGesture(minimumDistance: 30)
                            .onEnded { value in
                                let threshold: CGFloat = 50
                                let tabs = BottomTab.allCases
                                
                                guard let currentIndex = tabs.firstIndex(of: selectedTab) else { return }
                                
                                withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                                    if value.translation.width < -threshold && currentIndex < tabs.count - 1 {
                                        selectedTab = tabs[currentIndex + 1]
                                    } else if value.translation.width > threshold && currentIndex > 0 {
                                        selectedTab = tabs[currentIndex - 1]
                                    }
                                }
                            }
                    )
                    
                    BottomTabBar(selectedTab: $selectedTab)
                }
                .ignoresSafeArea(.keyboard)
                .sheet(isPresented: $showSettings) {
                    AyarlarView(isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                }
                .sheet(isPresented: $showAuth) {
                    AuthView(isLoggedIn: $isLoggedIn, userEmail: $userEmail)
                }
                .transition(.opacity)
            }
        }
    }
}

// ==================== INTRO EKRANI (Animasyonlu PNG Robot) ====================
struct IntroView: View {
    var onStart: () -> Void
    
    @State private var isGlowing = false
    @State private var isFloating = false
    @State private var isBreathing = false
    
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.04, green: 0.05, blue: 0.07),
                    Color(red: 0.07, green: 0.08, blue: 0.10),
                    Color(red: 0.03, green: 0.04, blue: 0.06)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack {
                HStack(spacing: 18) {
                    ForEach(0..<7) { i in
                        RoundedRectangle(cornerRadius: 3)
                            .fill(Color.white.opacity(0.04 + Double(i % 3) * 0.01))
                            .frame(width: 32, height: 160 + CGFloat(i % 4) * 20)
                    }
                }
                .offset(y: -80)
                .blur(radius: 0.5)
                
                Spacer()
            }
            
            Text("FOCUSPATH")
                .font(.system(size: 48, weight: .bold))
                .foregroundColor(.white.opacity(0.06))
                .offset(y: 40)
            
            VStack(spacing: 0) {
                Spacer()
                
                ZStack {
                    Circle()
                        .fill(
                            RadialGradient(
                                colors: [
                                    Color.cyan.opacity(isGlowing ? 0.35 : 0.08),
                                    Color.blue.opacity(isGlowing ? 0.2 : 0.0),
                                    Color.clear
                                ],
                                center: .center,
                                startRadius: 10,
                                endRadius: isGlowing ? 190 : 120
                            )
                        )
                        .frame(width: 380, height: 380)
                        .scaleEffect(isBreathing ? 1.08 : 0.94)
                    
                    Image("Robot")
                        .resizable()
                        .scaledToFit()
                        .frame(maxWidth: 320, maxHeight: 320)
                        .shadow(
                            color: Color.cyan.opacity(isGlowing ? 0.9 : 0.25),
                            radius: isGlowing ? 28 : 10
                        )
                        .shadow(
                            color: Color.blue.opacity(isGlowing ? 0.6 : 0.1),
                            radius: isGlowing ? 45 : 15
                        )
                        .scaleEffect(isBreathing ? 1.03 : 0.97)
                        .offset(y: isFloating ? -12 : 8)
                }
                .padding(.bottom, 30)
                .onAppear {
                    withAnimation(
                        .easeInOut(duration: 1.1)
                        .repeatForever(autoreverses: true)
                    ) {
                        isGlowing = true
                    }
                    
                    withAnimation(
                        .easeInOut(duration: 2.2)
                        .repeatForever(autoreverses: true)
                    ) {
                        isFloating = true
                    }
                    
                    withAnimation(
                        .easeInOut(duration: 1.6)
                        .repeatForever(autoreverses: true)
                    ) {
                        isBreathing = true
                    }
                }
                
                Text("ODAKLANMAYA HAZIR")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)
                
                Text("MISIN?")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.bottom, 14)
                
                Text("FocusPath ile Hedefine Ulaş")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(Color.white.opacity(0.65))
                    .padding(.bottom, 55)
                
                Button(action: onStart) {
                    Text("EVET, BAŞLA")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 18)
                        .background(
                            RoundedRectangle(cornerRadius: 14)
                                .fill(Color(red: 0.15, green: 0.45, blue: 0.95))
                        )
                        .shadow(color: Color.blue.opacity(0.45), radius: 12, y: 6)
                }
                .padding(.horizontal, 48)
                
                Spacer()
                Spacer()
            }
        }
    }
}

// MARK: - Alt Tab
enum BottomTab: String, CaseIterable {
    case anaSayfa = "Ana Sayfa"
    case gorevler = "Görevler"
    case oyunlar = "Oyunlar"
    case takvim = "Takvim"
    case ofis = "Ofis"
    case su = "Su"
    
    var icon: String {
        switch self {
        case .anaSayfa: return "house.fill"
        case .gorevler: return "checkmark.circle"
        case .oyunlar: return "gamecontroller"
        case .takvim: return "calendar"
        case .ofis: return "building.2"
        case .su: return "drop"
        }
    }
}

struct BottomTabBar: View {
    @Binding var selectedTab: BottomTab
    
    var body: some View {
        HStack {
            ForEach(BottomTab.allCases, id: \.self) { tab in
                Button {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        selectedTab = tab
                    }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: tab.icon)
                            .font(.system(size: 20))
                        Text(tab.rawValue)
                            .font(.system(size: 10, weight: .medium))
                    }
                    .foregroundColor(selectedTab == tab ? .white : .gray)
                    .frame(maxWidth: .infinity)
                }
            }
        }
        .padding(.top, 10)
        .padding(.bottom, 20)
        .background(Color(red: 0.07, green: 0.08, blue: 0.09))
    }
}

// ==================== DOPAMİN MENÜSÜ ====================
struct DopaminMenuView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            VStack(spacing: 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("DOPAMİN MENÜSÜ")
                            .font(.title3.weight(.bold))
                            .foregroundColor(.green)
                        
                        Text("Sosyal medya yerine sağlıklı mola seçenekleri")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    
                    Spacer()
                    
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.title3)
                            .foregroundColor(.white)
                            .padding(8)
                    }
                }
                .padding()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Mezelar (2-5 dk)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(.green)
                            
                            DopaminItem(icon: "wind", title: "Derin nefes al (2 dk)")
                            DopaminItem(icon: "window.vertical.open", title: "Pencereden dışarı bak")
                        }
                        
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Ana Yemekler (15-20 dk)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(.green)
                            
                            DopaminItem(icon: "figure.walk", title: "Kısa bir yürüyüş")
                            DopaminItem(icon: "book.fill", title: "10 sayfa kitap oku")
                            DopaminItem(icon: "broom", title: "Masayı topla")
                        }
                        
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Tatlılar (Ödüller)")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(.green)
                            
                            DopaminItem(icon: "music.note", title: "En sevdiğin şarkıyı dinle")
                            DopaminItem(icon: "apple.logo", title: "Bir meyve ye")
                        }
                        
                        Spacer().frame(height: 40)
                    }
                    .padding(.horizontal)
                }
            }
        }
    }
}

struct DopaminItem: View {
    let icon: String
    let title: String
    
    var body: some View {
        Button {
        } label: {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundColor(.white)
                    .frame(width: 28)
                
                Text(title)
                    .font(.subheadline)
                    .foregroundColor(.white)
                
                Spacer()
            }
            .padding()
            .background(Color(red: 0.14, green: 0.15, blue: 0.17))
            .cornerRadius(12)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// ==================== AUTH (LOGIN / SIGN UP) ====================
struct AuthView: View {
    @Environment(\.dismiss) var dismiss
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var isLoginMode = true
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var showError = false
    @State private var errorMessage = ""
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
                
                VStack(spacing: 24) {
                    VStack(spacing: 8) {
                        Circle()
                            .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 80, height: 80)
                            .overlay(
                                Image(systemName: "person.fill")
                                    .font(.system(size: 36))
                                    .foregroundColor(.white)
                            )
                        
                        Text(isLoginMode ? "Giriş Yap" : "Kayıt Ol")
                            .font(.title.bold())
                            .foregroundColor(.white)
                        
                        Text(isLoginMode ? "Hesabına giriş yap" : "Yeni hesap oluştur")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                    }
                    .padding(.top, 40)
                    
                    VStack(spacing: 16) {
                        TextField("E-posta", text: $email)
                            .padding()
                            .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                            .cornerRadius(12)
                            .foregroundColor(.white)
                            .autocapitalization(.none)
                            .keyboardType(.emailAddress)
                        
                        SecureField("Şifre", text: $password)
                            .padding()
                            .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                            .cornerRadius(12)
                            .foregroundColor(.white)
                        
                        if !isLoginMode {
                            SecureField("Şifre Tekrar", text: $confirmPassword)
                                .padding()
                                .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                                .cornerRadius(12)
                                .foregroundColor(.white)
                        }
                    }
                    .padding(.horizontal)
                    
                    Button {
                        handleAuth()
                    } label: {
                        Text(isLoginMode ? "Giriş Yap" : "Kayıt Ol")
                            .font(.headline.weight(.bold))
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color.green)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal)
                    
                    Button {
                        withAnimation {
                            isLoginMode.toggle()
                            errorMessage = ""
                        }
                    } label: {
                        Text(isLoginMode ? "Hesabın yok mu? Kayıt Ol" : "Zaten hesabın var mı? Giriş Yap")
                            .font(.subheadline)
                            .foregroundColor(.green)
                    }
                    
                    Spacer()
                }
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Kapat") { dismiss() }
                        .foregroundColor(.green)
                }
            }
            .alert("Hata", isPresented: $showError) {
                Button("Tamam", role: .cancel) { }
            } message: {
                Text(errorMessage)
            }
        }
    }
    
    private func handleAuth() {
        guard !email.trimmingCharacters(in: .whitespaces).isEmpty,
              !password.isEmpty else {
            errorMessage = "E-posta ve şifre boş olamaz"
            showError = true
            return
        }
        
        if !isLoginMode {
            guard password == confirmPassword else {
                errorMessage = "Şifreler eşleşmiyor"
                showError = true
                return
            }
        }
        
        userEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        isLoggedIn = true
        dismiss()
    }
}

// ==================== AYARLAR VIEW ====================
struct AyarlarView: View {
    @Environment(\.dismiss) var dismiss
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var terminalMode = true
    @State private var vibration = true
    @State private var timeAwareness = true
    @State private var sessionEnd = true
    @State private var autoDnd = true
    @State private var rainSound = false
    @State private var fireplaceSound = false
    @State private var alarmVolume: Double = 0.33
    @State private var signalInterval: Double = 60
    
    var body: some View {
        NavigationView {
            ZStack {
                Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
                
                ScrollView {
                    VStack(spacing: 16) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("FocusPath Premium")
                                    .font(.headline)
                                    .foregroundColor(.yellow)
                                Text("Reklamları kaldırır, mini oyunlara ve özel özelliklere erişim sağlar.")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                            Spacer()
                            Button("Hemen Yükselt") {}
                                .font(.subheadline.weight(.bold))
                                .foregroundColor(.black)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(Color.yellow)
                                .cornerRadius(8)
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.yellow.opacity(0.5), lineWidth: 1))
                        .cornerRadius(12)
                        
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Image(systemName: "flame.fill")
                                        .foregroundColor(.orange)
                                    Text("BEDAVA ÖDÜL KAZAN")
                                        .font(.subheadline.weight(.bold))
                                        .foregroundColor(.white)
                                }
                                Text("Kısa bir video izleyerek anında +50 Jeton ve +20 XP kazan!")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }
                            Spacer()
                            Button("İzle & Kazan") {}
                                .font(.subheadline.weight(.bold))
                                .foregroundColor(.black)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 8)
                                .background(Color.green)
                                .cornerRadius(8)
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(12)
                        
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Sistem Görünümü")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            Toggle(isOn: $terminalMode) {
                                Text("Terminal Modu")
                                    .foregroundColor(.white)
                            }
                            .tint(.green)
                            
                            HStack {
                                Text("Görüntüleme Dili")
                                    .foregroundColor(.white)
                                Spacer()
                                Text("Türkçe")
                                    .foregroundColor(.gray)
                            }
                            
                            Toggle(isOn: $vibration) {
                                Text("Titreşim Geri Bildirimi")
                                    .foregroundColor(.white)
                            }
                            .tint(.green)
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(12)
                        
                        VStack(alignment: .leading, spacing: 14) {
                            Text("DEHB ARAÇLARI")
                                .font(.headline)
                                .foregroundColor(.green)
                            
                            Toggle(isOn: $timeAwareness) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Zaman Farkındalığı Sinyalleri")
                                        .foregroundColor(.white)
                                    Text("Her 60 dakikada bir hafif uyarı")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                            }
                            .tint(.green)
                            
                            Text("Sinyal Aralığı: \(Int(signalInterval)) dk")
                                .font(.caption)
                                .foregroundColor(.gray)
                            
                            Slider(value: $signalInterval, in: 15...120, step: 15)
                                .tint(.green)
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(12)
                        
                        VStack(alignment: .leading, spacing: 14) {
                            Text("BİLDİRİM VE SES")
                                .font(.headline)
                                .foregroundColor(.green)
                            
                            Toggle(isOn: $sessionEnd) {
                                Text("Seans Bitiş Bildirimi")
                                    .foregroundColor(.white)
                            }
                            .tint(.green)
                            
                            Toggle(isOn: $autoDnd) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Otomatik Rahatsız Etmeyin")
                                        .foregroundColor(.white)
                                    Text("Odaklanınca moda otomatik aç")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                            }
                            .tint(.green)
                            
                            Text("AMBİYANS SESLERİ (DÖNGÜ)")
                                .font(.caption)
                                .foregroundColor(.gray)
                                .padding(.top, 4)
                            
                            Toggle(isOn: $rainSound) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Ambiyans Yağmuru")
                                        .foregroundColor(.white)
                                    Text("Odaklanırken yağmur sesi çalar")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                            }
                            .tint(.green)
                            
                            Toggle(isOn: $fireplaceSound) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Ambiyans Şöminesi")
                                        .foregroundColor(.white)
                                    Text("Odaklanırken çıtırı sesi çalar")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                            }
                            .tint(.green)
                            
                            Text("Alarm Ses Seviyesi: \(Int(alarmVolume * 100))%")
                                .font(.caption)
                                .foregroundColor(.gray)
                            
                            Slider(value: $alarmVolume, in: 0...1)
                                .tint(.green)
                            
                            Text("Alarm Sesi")
                                .font(.caption)
                                .foregroundColor(.gray)
                            
                            HStack(spacing: 20) {
                                Image(systemName: "music.note")
                                    .font(.title2)
                                    .foregroundColor(.gray)
                                Image(systemName: "bell.fill")
                                    .font(.title2)
                                    .foregroundColor(.gray)
                                Image(systemName: "light.beacon.max.fill")
                                    .font(.title2)
                                    .foregroundColor(.red)
                            }
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(12)
                        
                        VStack(alignment: .leading, spacing: 14) {
                            Text("Kullanıcı Hesabı")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            HStack {
                                Circle()
                                    .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                                    .frame(width: 44, height: 44)
                                
                                VStack(alignment: .leading, spacing: 2) {
                                    HStack {
                                        Text(isLoggedIn ? (userEmail.components(separatedBy: "@").first ?? "Kullanıcı") : "Misafir")
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundColor(.white)
                                        Image(systemName: "pencil")
                                            .font(.caption)
                                            .foregroundColor(.gray)
                                    }
                                    Text(isLoggedIn ? userEmail : "Giriş yapılmadı")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                                
                                Button("Değiştir") {}
                                    .font(.subheadline)
                                    .foregroundColor(.green)
                            }
                            
                            Button {
                                isLoggedIn = false
                                userEmail = ""
                                dismiss()
                            } label: {
                                Text(isLoggedIn ? "Bağlantıyı Kes" : "Giriş Yap")
                                    .font(.headline.weight(.bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 14)
                                    .background(isLoggedIn ? Color.red.opacity(0.8) : Color.green)
                                    .cornerRadius(12)
                            }
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(12)
                        
                        Spacer().frame(height: 40)
                    }
                    .padding()
                }
            }
            .navigationTitle("Ayarlar")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Kapat") { dismiss() }
                        .foregroundColor(.green)
                }
            }
        }
    }
}

// MARK: - ANA SAYFA
struct AnaSayfaView: View {
    @Binding var showSettings: Bool
    @Binding var showAuth: Bool
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var selectedTopTab = 0
    @State private var showAlert = false
    @State private var alertMessage = ""
    @State private var showDopaminMenu = false
    
    @State private var notes: [String] = UserDefaults.standard.stringArray(forKey: "notes") ?? ["İlk notum", "Odaklanma hedefleri"]
    @State private var newNote = ""
    @State private var showAddNote = false
    
    @StateObject private var audio = AudioManager.shared
    
    let topTabs = ["GÖREV", "GELEN", "BAŞARIM", "NOTLAR"]
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            VStack(spacing: 0) {
                HStack {
                    Text("Ana Sayfa")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    HStack(spacing: 18) {
                        Button { showMessage("Arama açıldı") } label: { Image(systemName: "magnifyingglass") }
                        Button { showMessage("Bildirimler") } label: { Image(systemName: "bell") }
                        Button { showSettings = true } label: { Image(systemName: "gearshape") }
                        
                        Button {
                            showAuth = true
                        } label: {
                            Circle()
                                .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 28, height: 28)
                        }
                    }
                    .foregroundColor(.white)
                    .font(.system(size: 18))
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 12)
                
                HStack(spacing: 0) {
                    ForEach(0..<topTabs.count, id: \.self) { index in
                        Button {
                            withAnimation { selectedTopTab = index }
                        } label: {
                            Text(topTabs[index])
                                .font(.caption.weight(.semibold))
                                .foregroundColor(selectedTopTab == index ? .green : .gray)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .overlay(
                                    Rectangle()
                                        .fill(selectedTopTab == index ? Color.green : Color.clear)
                                        .frame(height: 2)
                                        .padding(.top, 28),
                                    alignment: .bottom
                                )
                        }
                    }
                }
                .padding(.horizontal)
                
                HStack {
                    Text(isLoggedIn ? "Bağlı: \(userEmail)" : "Bağlı değil")
                        .font(.caption)
                        .foregroundColor(.gray)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 8)
                
                ScrollView {
                    Group {
                        if selectedTopTab == 0 || selectedTopTab == 1 {
                            VStack(spacing: 16) {
                                HStack {
                                    Circle()
                                        .fill(LinearGradient(colors: [.green, .teal], startPoint: .topLeading, endPoint: .bottomTrailing))
                                        .frame(width: 36, height: 36)
                                    
                                    Text("Ana Sayfa")
                                        .font(.headline)
                                        .foregroundColor(.white)
                                    
                                    Spacer()
                                    
                                    Text("4 AKTİF")
                                        .font(.caption.weight(.semibold))
                                        .foregroundColor(.black)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 4)
                                        .background(Color.green)
                                        .clipShape(Capsule())
                                }
                                .padding()
                                .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                                .cornerRadius(12)
                                .padding(.horizontal)
                                
                                VStack(alignment: .leading, spacing: 16) {
                                    HStack {
                                        Image(systemName: "radio")
                                            .foregroundColor(.green)
                                            .font(.title3)
                                        Text("ODAK RADYOSU")
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundColor(.white)
                                    }
                                    
                                    HStack(spacing: 18) {
                                        RadioButton(icon: "headphones", title: "Odak", isActive: audio.currentSound == "odak") {
                                            audio.play(named: "odak", fileName: "odak")
                                        }
                                        RadioButton(icon: "cup.and.saucer.fill", title: "Kahve", isActive: audio.currentSound == "kahve") {
                                            audio.play(named: "kahve", fileName: "kahve")
                                        }
                                        RadioButton(icon: "brain.head.profile", title: "Beyin", isActive: audio.currentSound == "beyin") {
                                            audio.play(named: "beyin", fileName: "beyin")
                                        }
                                        RadioButton(icon: "cloud.rain", title: "Yağmur", isActive: audio.currentSound == "yagmur") {
                                            audio.play(named: "yagmur", fileName: "yagmur")
                                        }
                                        RadioButton(icon: "flame.fill", title: "Ateş", isActive: audio.currentSound == "ates") {
                                            audio.play(named: "ates", fileName: "ates")
                                        }
                                    }
                                    .frame(maxWidth: .infinity)
                                }
                                .padding(18)
                                .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                                .cornerRadius(14)
                                .padding(.horizontal)
                                
                                Button {
                                    showDopaminMenu = true
                                } label: {
                                    HStack {
                                        Image(systemName: "cup.and.saucer.fill")
                                            .foregroundColor(.green)
                                        
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("DOPAMİN MENÜSÜ")
                                                .font(.subheadline.weight(.semibold))
                                                .foregroundColor(.white)
                                            Text("Molaya mı ihtiyacın var? Sağlıklı bir aktivite seç.")
                                                .font(.caption)
                                                .foregroundColor(.gray)
                                                .multilineTextAlignment(.leading)
                                        }
                                        
                                        Spacer()
                                        
                                        Image(systemName: "chevron.right")
                                            .foregroundColor(.gray)
                                    }
                                    .padding()
                                    .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                                    .cornerRadius(12)
                                }
                                .padding(.horizontal)
                                
                                VStack(alignment: .leading, spacing: 12) {
                                    HStack {
                                        Text("LİDERLİK TABLOSU")
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundColor(.white)
                                        
                                        Spacer()
                                        
                                        Text("OYUNCULAR")
                                            .font(.caption)
                                            .foregroundColor(.green)
                                        
                                        Text("TAKIMLAR")
                                            .font(.caption)
                                            .foregroundColor(.gray)
                                            .padding(.leading, 8)
                                    }
                                    
                                    HStack(spacing: 20) {
                                        LeaderItem(name: "AI", xp: "7082 XP", isGradient: true)
                                        LeaderItem(name: "Elif", xp: "5388 XP")
                                        LeaderItem(name: "Zeynep", xp: "4396 XP")
                                    }
                                    .frame(maxWidth: .infinity)
                                }
                                .padding()
                                .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                                .cornerRadius(12)
                                .padding(.horizontal)
                                
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("ÖDÜLLER VE İLERLEME")
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(.white)
                                    
                                    HStack {
                                        StatBox(icon: "building.columns.fill", value: "225", label: "Coin", color: .yellow)
                                        
                                        Divider().frame(height: 50).background(Color.gray.opacity(0.3))
                                        
                                        VStack(spacing: 4) {
                                            Image(systemName: "star.fill")
                                                .foregroundColor(.yellow)
                                                .font(.title2)
                                            Text("Stajyer (Garaj Girişimi) / 5")
                                                .font(.caption2)
                                                .foregroundColor(.white)
                                                .multilineTextAlignment(.center)
                                            Text("Şirket Unvanı / Seviyesi")
                                                .font(.caption2)
                                                .foregroundColor(.gray)
                                        }
                                        .frame(maxWidth: .infinity)
                                        
                                        Divider().frame(height: 50).background(Color.gray.opacity(0.3))
                                        
                                        StatBox(icon: "flame.fill", value: "5", label: "Seri", color: .orange)
                                    }
                                }
                                .padding()
                                .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                                .cornerRadius(12)
                                .padding(.horizontal)
                                
                                HStack {
                                    Text("ODAKLANMA SEANSLARI (BUGÜN)")
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(.white)
                                    Spacer()
                                }
                                .padding(.horizontal)
                                
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text("Ozdisan Electro")
                                            .font(.caption)
                                            .foregroundColor(.gray)
                                        Text("1.000.000+ Komponent")
                                            .font(.subheadline.weight(.semibold))
                                            .foregroundColor(.white)
                                    }
                                    
                                    Spacer()
                                    
                                    Button {
                                        showMessage("Reklam açıldı")
                                    } label: {
                                        Text("Aç")
                                            .font(.subheadline.weight(.bold))
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 20)
                                            .padding(.vertical, 8)
                                            .background(Color.blue)
                                            .cornerRadius(8)
                                    }
                                }
                                .padding()
                                .background(Color.white.opacity(0.08))
                                .cornerRadius(12)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 12)
                                        .stroke(Color.gray.opacity(0.3), lineWidth: 1)
                                )
                                .padding(.horizontal)
                            }
                            .padding(.top, 12)
                            
                        } else if selectedTopTab == 2 {
                            VStack(spacing: 16) {
                                Text("BAŞARIMLAR")
                                    .font(.title2.bold())
                                    .foregroundColor(.green)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .padding(.horizontal)
                                
                                VStack(spacing: 12) {
                                    AchievementCard(title: "İlk Adım", desc: "İlk görevi tamamla", isUnlocked: true, icon: "star.fill")
                                    AchievementCard(title: "Odak Ustası", desc: "5 seans tamamla", isUnlocked: true, icon: "brain.head.profile")
                                    AchievementCard(title: "Su İçen", desc: "8 bardak su iç", isUnlocked: false, icon: "drop.fill")
                                    AchievementCard(title: "Seri Koruyucu", desc: "7 gün üst üste", isUnlocked: false, icon: "flame.fill")
                                    AchievementCard(title: "Oyun Sever", desc: "10 mini oyun oyna", isUnlocked: false, icon: "gamecontroller.fill")
                                }
                                .padding(.horizontal)
                            }
                            .padding(.top, 12)
                            
                        } else if selectedTopTab == 3 {
                            VStack(spacing: 16) {
                                HStack {
                                    Text("NOTLAR")
                                        .font(.title2.bold())
                                        .foregroundColor(.green)
                                    Spacer()
                                    Button {
                                        showAddNote = true
                                    } label: {
                                        Image(systemName: "plus.circle.fill")
                                            .font(.title2)
                                            .foregroundColor(.green)
                                    }
                                }
                                .padding(.horizontal)
                                
                                if notes.isEmpty {
                                    Text("Henüz not yok. + ile ekle.")
                                        .foregroundColor(.gray)
                                        .padding()
                                } else {
                                    ForEach(notes.indices, id: \.self) { index in
                                        HStack {
                                            Text(notes[index])
                                                .foregroundColor(.white)
                                            Spacer()
                                            Button {
                                                notes.remove(at: index)
                                                UserDefaults.standard.set(notes, forKey: "notes")
                                            } label: {
                                                Image(systemName: "trash")
                                                    .foregroundColor(.red)
                                            }
                                        }
                                        .padding()
                                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                                        .cornerRadius(12)
                                        .padding(.horizontal)
                                    }
                                }
                            }
                            .padding(.top, 12)
                        }
                    }
                    .padding(.bottom, 100)
                }
            }
        }
        .alert(alertMessage, isPresented: $showAlert) {
            Button("Tamam", role: .cancel) { }
        }
        .sheet(isPresented: $showDopaminMenu) {
            DopaminMenuView()
        }
        .alert("Yeni Not", isPresented: $showAddNote) {
            TextField("Notunu yaz...", text: $newNote)
            Button("Ekle") {
                let trimmed = newNote.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty {
                    notes.insert(trimmed, at: 0)
                    UserDefaults.standard.set(notes, forKey: "notes")
                    newNote = ""
                }
            }
            Button("İptal", role: .cancel) { newNote = "" }
        }
    }
    
    private func showMessage(_ message: String) {
        alertMessage = message
        showAlert = true
    }
}

// MARK: - GÖREVLER VIEW
struct GorevlerView: View {
    @Binding var showSettings: Bool
    @Binding var showAuth: Bool
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var selectedTopTab = 0
    @State private var taskTitle = ""
    @State private var taskNotes = ""
    @State private var taskDuration = ""
    @State private var selectedCategory = "Genel"
    @State private var selectedDifficulty = "ORTA"
    @State private var searchText = ""
    @State private var tasks: [TaskItem] = []
    @State private var habits: [HabitItem] = []
    @State private var newHabitText = ""
    @State private var showAddHabitAlert = false
    @State private var showAlert = false
    @State private var alertMessage = ""
    
    let topTabs = ["GÖREV", "GELEN", "BAŞARIM", "NOTLAR"]
    let categories = ["Genel", "İş", "Kod", "Okul"]
    let difficulties = ["KOLAY", "ORTA", "ZOR"]
    
    var filteredTasks: [TaskItem] {
        if searchText.isEmpty { return tasks }
        return tasks.filter {
            $0.title.localizedCaseInsensitiveContains(searchText) ||
            $0.notes.localizedCaseInsensitiveContains(searchText)
        }
    }
    
    var progress: Double {
        guard !tasks.isEmpty else { return 0 }
        let completed = tasks.filter { $0.isCompleted }.count
        return Double(completed) / Double(tasks.count)
    }
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            VStack(spacing: 0) {
                HStack {
                    Text("Görevler")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    HStack(spacing: 18) {
                        Button { showMessage("Arama") } label: { Image(systemName: "magnifyingglass") }
                        Button { showMessage("Bildirimler") } label: { Image(systemName: "bell") }
                        Button { showSettings = true } label: { Image(systemName: "gearshape") }
                        
                        Button {
                            showAuth = true
                        } label: {
                            Circle()
                                .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 28, height: 28)
                        }
                    }
                    .foregroundColor(.white)
                    .font(.system(size: 18))
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 12)
                
                HStack(spacing: 0) {
                    ForEach(0..<topTabs.count, id: \.self) { index in
                        Button {
                            withAnimation { selectedTopTab = index }
                        } label: {
                            Text(topTabs[index])
                                .font(.caption.weight(.semibold))
                                .foregroundColor(selectedTopTab == index ? .green : .gray)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .overlay(
                                    Rectangle()
                                        .fill(selectedTopTab == index ? Color.green : Color.clear)
                                        .frame(height: 2)
                                        .padding(.top, 28),
                                    alignment: .bottom
                                )
                        }
                    }
                }
                .padding(.horizontal)
                
                HStack {
                    Text(isLoggedIn ? "Bağlı: \(userEmail)" : "Bağlı değil")
                        .font(.caption)
                        .foregroundColor(.gray)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 8)
                
                ScrollView {
                    VStack(spacing: 16) {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Image(systemName: "flame.fill")
                                    .foregroundColor(.red)
                                Text("ALIŞKANLIK ZİNCİRİ")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(.white)
                                Spacer()
                                
                                Button {
                                    showAddHabitAlert = true
                                } label: {
                                    Image(systemName: "plus.circle.fill")
                                        .foregroundColor(.green)
                                        .font(.title3)
                                }
                            }
                            
                            if habits.isEmpty {
                                Text("Zinciri kırma! İlk alışkanlığını ekle.")
                                    .font(.subheadline)
                                    .foregroundColor(.gray)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                                    .background(Color(red: 0.14, green: 0.15, blue: 0.17))
                                    .cornerRadius(10)
                            } else {
                                ForEach(habits) { habit in
                                    HStack {
                                        Image(systemName: "flame.fill")
                                            .foregroundColor(.orange)
                                        
                                        Text(habit.title)
                                            .foregroundColor(.white)
                                        
                                        Spacer()
                                        
                                        Button {
                                            withAnimation {
                                                habits.removeAll { $0.id == habit.id }
                                                saveHabits()
                                            }
                                        } label: {
                                            Image(systemName: "trash")
                                                .foregroundColor(.red.opacity(0.8))
                                                .font(.body)
                                        }
                                    }
                                    .padding(12)
                                    .background(Color(red: 0.14, green: 0.15, blue: 0.17))
                                    .cornerRadius(10)
                                }
                            }
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(14)
                        .padding(.horizontal)
                        
                        VStack(alignment: .leading, spacing: 10) {
                            HStack {
                                RoundedRectangle(cornerRadius: 6)
                                    .fill(Color.orange)
                                    .frame(width: 28, height: 28)
                                    .overlay(Image(systemName: "sun.max.fill").foregroundColor(.white).font(.caption))
                                
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Günaydın")
                                        .font(.headline)
                                        .foregroundColor(.white)
                                    Text("\(isLoggedIn ? userEmail : "misafir") | Stajyer (Garaj...")
                                        .font(.caption2)
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                                
                                HStack(spacing: 8) {
                                    StatBadge(title: "Toplam", value: "\(tasks.count)")
                                    StatBadge(title: "Biten", value: "\(tasks.filter { $0.isCompleted }.count)")
                                    StatBadge(title: "5d", value: "", isStreak: true)
                                }
                            }
                            
                            Text("Disiplin nefret ettiğin her şeyi yapmak ama onu seviyormuş gibi yapmaktır. ...")
                                .font(.caption)
                                .foregroundColor(.gray)
                                .lineLimit(2)
                            
                            HStack {
                                Image(systemName: "scope")
                                    .foregroundColor(.green)
                                Text("GÜNÜN ODAĞI: Neye odaklanac...")
                                    .font(.caption)
                                    .foregroundColor(.white)
                            }
                            .padding(.top, 4)
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(14)
                        .padding(.horizontal)
                        
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text("İlerleme")
                                    .font(.subheadline)
                                    .foregroundColor(.white)
                                Spacer()
                                Text("\(Int(progress * 100))%")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(.green)
                            }
                            
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    Capsule()
                                        .fill(Color.gray.opacity(0.3))
                                        .frame(height: 6)
                                    Capsule()
                                        .fill(Color.green)
                                        .frame(width: geo.size.width * progress, height: 6)
                                }
                            }
                            .frame(height: 6)
                        }
                        .padding(.horizontal)
                        
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Yeni Görev Ekle")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            HStack(spacing: 12) {
                                Image(systemName: "pencil")
                                    .foregroundColor(.green)
                                TextField("Görev Başlığı", text: $taskTitle)
                                    .foregroundColor(.white)
                            }
                            .padding()
                            .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                            .cornerRadius(12)
                            
                            HStack(spacing: 12) {
                                HStack(spacing: 12) {
                                    Image(systemName: "note.text")
                                        .foregroundColor(.blue)
                                    TextField("Notlar", text: $taskNotes)
                                        .foregroundColor(.white)
                                }
                                .padding()
                                .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                                .cornerRadius(12)
                                
                                HStack(spacing: 8) {
                                    Image(systemName: "clock")
                                        .foregroundColor(.orange)
                                    TextField("Süre", text: $taskDuration)
                                        .foregroundColor(.white)
                                        .frame(width: 50)
                                }
                                .padding()
                                .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                                .cornerRadius(12)
                            }
                            
                            Text("Kategori")
                                .font(.caption)
                                .foregroundColor(.gray)
                            
                            HStack(spacing: 8) {
                                ForEach(categories, id: \.self) { cat in
                                    Button {
                                        withAnimation(.spring(response: 0.3)) {
                                            selectedCategory = cat
                                        }
                                    } label: {
                                        Text(cat)
                                            .font(.caption.weight(.semibold))
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 9)
                                            .background(
                                                selectedCategory == cat ?
                                                Color.green.opacity(0.25) :
                                                Color(red: 0.18, green: 0.19, blue: 0.21)
                                            )
                                            .foregroundColor(selectedCategory == cat ? .green : .white)
                                            .cornerRadius(10)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(selectedCategory == cat ? Color.green.opacity(0.6) : Color.clear, lineWidth: 1)
                                            )
                                    }
                                }
                            }
                            
                            Text("Zorluk")
                                .font(.caption)
                                .foregroundColor(.gray)
                            
                            HStack(spacing: 10) {
                                ForEach(difficulties, id: \.self) { diff in
                                    Button {
                                        withAnimation(.spring(response: 0.3)) {
                                            selectedDifficulty = diff
                                        }
                                    } label: {
                                        Text(diff)
                                            .font(.caption.weight(.bold))
                                            .frame(maxWidth: .infinity)
                                            .padding(.vertical, 10)
                                            .background(
                                                selectedDifficulty == diff ?
                                                Color.purple.opacity(0.3) :
                                                Color(red: 0.18, green: 0.19, blue: 0.21)
                                            )
                                            .foregroundColor(selectedDifficulty == diff ? .purple : .white)
                                            .cornerRadius(10)
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(selectedDifficulty == diff ? Color.purple.opacity(0.6) : Color.clear, lineWidth: 1)
                                            )
                                    }
                                }
                            }
                            
                            Button {
                                addTask()
                            } label: {
                                HStack {
                                    Image(systemName: "plus.circle.fill")
                                        .font(.title3)
                                    Text("GÖREVİ EKLE")
                                        .fontWeight(.bold)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(
                                    LinearGradient(colors: [Color.green, Color.green.opacity(0.8)],
                                                   startPoint: .leading, endPoint: .trailing)
                                )
                                .foregroundColor(.black)
                                .cornerRadius(14)
                                .shadow(color: Color.green.opacity(0.3), radius: 8, y: 4)
                            }
                            .padding(.top, 8)
                        }
                        .padding(18)
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(16)
                        .padding(.horizontal)
                        
                        if !filteredTasks.isEmpty {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("Görevlerim")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(.white)
                                    .padding(.horizontal)
                                
                                ForEach(filteredTasks) { task in
                                    TaskRow(task: task) {
                                        toggleTask(task)
                                    }
                                }
                            }
                            .padding(.horizontal)
                        }
                        
                        Spacer().frame(height: 120)
                    }
                    .padding(.top, 12)
                }
                
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.gray)
                    TextField("Görevlerde ara...", text: $searchText)
                        .foregroundColor(.white)
                }
                .padding()
                .background(Color(red: 0.14, green: 0.15, blue: 0.17))
                .cornerRadius(12)
                .padding(.horizontal)
                .padding(.bottom, 90)
            }
        }
        .onAppear {
            loadTasks()
            loadHabits()
        }
        .alert("Yeni Alışkanlık", isPresented: $showAddHabitAlert) {
            TextField("Alışkanlık adı", text: $newHabitText)
            Button("Ekle") {
                let trimmed = newHabitText.trimmingCharacters(in: .whitespaces)
                if !trimmed.isEmpty {
                    withAnimation {
                        habits.append(HabitItem(title: trimmed))
                        saveHabits()
                    }
                    newHabitText = ""
                }
            }
            Button("İptal", role: .cancel) {
                newHabitText = ""
            }
        }
        .alert(alertMessage, isPresented: $showAlert) {
            Button("Tamam", role: .cancel) { }
        }
    }
    
    private func addTask() {
        guard !taskTitle.trimmingCharacters(in: .whitespaces).isEmpty else {
            showMessage("Görev başlığı boş olamaz!")
            return
        }
        
        let newTask = TaskItem(
            title: taskTitle,
            notes: taskNotes,
            duration: taskDuration,
            category: selectedCategory,
            difficulty: selectedDifficulty
        )
        
        withAnimation {
            tasks.insert(newTask, at: 0)
            saveTasks()
        }
        
        taskTitle = ""
        taskNotes = ""
        taskDuration = ""
        selectedCategory = "Genel"
        selectedDifficulty = "ORTA"
        
        showMessage("Görev başarıyla eklendi!")
    }
    
    private func toggleTask(_ task: TaskItem) {
        if let index = tasks.firstIndex(where: { $0.id == task.id }) {
            tasks[index].isCompleted.toggle()
            saveTasks()
        }
    }
    
    private func showMessage(_ msg: String) {
        alertMessage = msg
        showAlert = true
    }
    
    private func loadTasks() {
        if let data = UserDefaults.standard.data(forKey: "tasks"),
           let decoded = try? JSONDecoder().decode([TaskItem].self, from: data) {
            tasks = decoded
        }
    }
    
    private func saveTasks() {
        if let encoded = try? JSONEncoder().encode(tasks) {
            UserDefaults.standard.set(encoded, forKey: "tasks")
        }
    }
    
    private func loadHabits() {
        if let data = UserDefaults.standard.data(forKey: "habits"),
           let decoded = try? JSONDecoder().decode([HabitItem].self, from: data) {
            habits = decoded
        }
    }
    
    private func saveHabits() {
        if let encoded = try? JSONEncoder().encode(habits) {
            UserDefaults.standard.set(encoded, forKey: "habits")
        }
    }
}

// MARK: - OYUNLAR VIEW
struct OyunlarView: View {
    @Binding var showSettings: Bool
    @Binding var showAuth: Bool
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var showAlert = false
    @State private var alertMessage = ""
    @State private var selectedGame: GameItem? = nil
    
    let freeGames: [GameItem] = [
        GameItem(icon: "paintpalette.fill", title: "Stroop Testi", subtitle: "Odaklanma ve Renk Algısı", isPremium: false),
        GameItem(icon: "arrow.triangle.turn.up.right.diamond.fill", title: "Labirent Serüveni", subtitle: "Planlama ve Stratejik Dikkat", isPremium: false),
        GameItem(icon: "puzzlepiece.fill", title: "Görsel Yapboz", subtitle: "Mekansal Zeka ve Odak", isPremium: false),
        GameItem(icon: "cube.fill", title: "Küp Takibi", subtitle: "Görsel Sayım ve Dikkat", isPremium: false),
        GameItem(icon: "target", title: "Okçu Terminali", subtitle: "Zamanlama ve Hedef Odaklama", isPremium: false),
        GameItem(icon: "grid", title: "Sudoku", subtitle: "Sayısal Mantık ve Odak", isPremium: false),
        GameItem(icon: "textformat.abc", title: "Kelime Avcısı", subtitle: "Sözel Zeka ve Hız", isPremium: false),
        GameItem(icon: "plus.forwardslash.minus", title: "Matematik Fırtınası", subtitle: "Dört İşlem ve Hız", isPremium: false)
    ]
    
    let premiumGames: [GameItem] = [
        GameItem(icon: "lock.fill", title: "Farkı Bul (Premium)", subtitle: "Görsel Tarama ve Dikkat", isPremium: true),
        GameItem(icon: "lock.fill", title: "Hafıza Matrisi (Premium)", subtitle: "Görsel-Mekansal Bellek", isPremium: true),
        GameItem(icon: "lock.fill", title: "Bardak Bulmacası (Premium)", subtitle: "Görsel Takip ve Odak", isPremium: true),
        GameItem(icon: "lock.fill", title: "IQ Testi (Premium)", subtitle: "Mantıksal Akıl Yürütme", isPremium: true),
        GameItem(icon: "lock.fill", title: "Desen Tekrarı (Premium)", subtitle: "Hafıza ve Sıralı Dikkat", isPremium: true)
    ]
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            VStack(spacing: 0) {
                HStack {
                    Text(isLoggedIn ? "Bağlı: \(userEmail)" : "Bağlı değil")
                        .font(.caption)
                        .foregroundColor(.gray)
                    Spacer()
                    
                    Button { showSettings = true } label: {
                        Image(systemName: "gearshape")
                            .foregroundColor(.white)
                    }
                    
                    Button {
                        showAuth = true
                    } label: {
                        Circle()
                            .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 28, height: 28)
                    }
                }
                .padding(.horizontal)
                .padding(.top, 12)
                
                HStack {
                    Image(systemName: "gamecontroller.fill")
                        .foregroundColor(.green)
                    Text("DİKKAT OYUNLARI")
                        .font(.title3.weight(.bold))
                        .foregroundColor(.green)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 16)
                
                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(freeGames) { game in
                            GameRow(game: game) {
                                selectedGame = game
                            }
                        }
                        
                        ForEach(premiumGames) { game in
                            GameRow(game: game) {
                                showMessage("Bu oyun Premium! Satın almanız gerekiyor.")
                            }
                        }
                        
                        Spacer().frame(height: 120)
                    }
                    .padding(.horizontal)
                }
            }
        }
        .alert(alertMessage, isPresented: $showAlert) {
            Button("Tamam", role: .cancel) { }
        }
        .fullScreenCover(item: $selectedGame) { game in
            GamePlayView(game: game)
        }
    }
    
    private func showMessage(_ msg: String) {
        alertMessage = msg
        showAlert = true
    }
}

struct GameItem: Identifiable {
    let id = UUID()
    let icon: String
    let title: String
    let subtitle: String
    let isPremium: Bool
}

struct GameRow: View {
    let game: GameItem
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: game.icon)
                    .font(.title3)
                    .foregroundColor(game.isPremium ? .gray : .green)
                    .frame(width: 36, height: 36)
                
                VStack(alignment: .leading, spacing: 3) {
                    Text(game.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(game.isPremium ? .gray : .white)
                    
                    Text(game.subtitle)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                
                Spacer()
                
                if game.isPremium {
                    Image(systemName: "chevron.right")
                        .foregroundColor(.gray)
                        .font(.caption)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(Color(red: 0.12, green: 0.14, blue: 0.16))
            .cornerRadius(12)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// ==================== OYUN OYNAMA EKRANLARI ====================
struct GamePlayView: View {
    @Environment(\.dismiss) var dismiss
    let game: GameItem
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            VStack {
                HStack {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title2)
                            .foregroundColor(.gray)
                    }
                    
                    Spacer()
                    
                    Text(game.title)
                        .font(.headline.weight(.bold))
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    Color.clear.frame(width: 28, height: 28)
                }
                .padding()
                
                switch game.title {
                case "Stroop Testi":
                    StroopTestView()
                case "Matematik Fırtınası":
                    MathStormView()
                case "Kelime Avcısı":
                    WordHunterView()
                case "Küp Takibi":
                    CubeTrackingView()
                case "Labirent Serüveni":
                    MazeGameView()
                case "Sudoku":
                    SudokuGameView()
                case "Okçu Terminali":
                    OkcuTerminaliView()
                case "Görsel Yapboz":
                    GorselYapbozView()
                default:
                    GenericGameView(title: game.title, subtitle: game.subtitle)
                }
            }
        }
    }
}

// ==================== STROOP TESTİ ====================
struct StroopTestView: View {
    @State private var score = 0
    @State private var currentWord = ""
    @State private var currentColor: Color = .red
    @State private var options: [Color] = []
    @State private var timeLeft = 30
    @State private var isPlaying = false
    @State private var timer: Timer? = nil
    
    let colors: [(String, Color)] = [
        ("KIRMIZI", .red),
        ("MAVİ", .blue),
        ("YEŞİL", .green),
        ("SARI", .yellow),
        ("TURUNCU", .orange),
        ("MOR", .purple)
    ]
    
    var body: some View {
        VStack(spacing: 30) {
            HStack {
                Text("Skor: \(score)")
                    .font(.title2.bold())
                    .foregroundColor(.green)
                Spacer()
                Text("Süre: \(timeLeft)")
                    .font(.title2.bold())
                    .foregroundColor(.orange)
            }
            .padding(.horizontal)
            
            if isPlaying {
                Text(currentWord)
                    .font(.system(size: 48, weight: .bold))
                    .foregroundColor(currentColor)
                    .padding(.vertical, 40)
                
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                    ForEach(0..<options.count, id: \.self) { i in
                        Button {
                            checkAnswer(options[i])
                        } label: {
                            RoundedRectangle(cornerRadius: 14)
                                .fill(options[i])
                                .frame(height: 70)
                        }
                    }
                }
                .padding(.horizontal)
            } else {
                VStack(spacing: 20) {
                    Text("Stroop Testi")
                        .font(.title.bold())
                        .foregroundColor(.white)
                    
                    Text("Kelimenin rengine tıkla,\nkelimenin kendisine değil!")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    
                    Button {
                        startGame()
                    } label: {
                        Text("BAŞLA")
                            .font(.headline.bold())
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal, 40)
                }
            }
            
            Spacer()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func startGame() {
        score = 0
        timeLeft = 30
        isPlaying = true
        nextQuestion()
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeLeft > 0 {
                timeLeft -= 1
            } else {
                isPlaying = false
                timer?.invalidate()
            }
        }
    }
    
    private func nextQuestion() {
        let wordItem = colors.randomElement()!
        var colorItem = colors.randomElement()!
        while colorItem.1 == wordItem.1 {
            colorItem = colors.randomElement()!
        }
        
        currentWord = wordItem.0
        currentColor = colorItem.1
        
        options = Array(colors.map { $0.1 }.shuffled().prefix(4))
        if !options.contains(currentColor) {
            options[0] = currentColor
            options.shuffle()
        }
    }
    
    private func checkAnswer(_ selected: Color) {
        if selected == currentColor {
            score += 1
        }
        nextQuestion()
    }
}

// ==================== MATEMATİK FIRTINASI ====================
struct MathStormView: View {
    @State private var score = 0
    @State private var question = ""
    @State private var correctAnswer = 0
    @State private var options: [Int] = []
    @State private var timeLeft = 30
    @State private var isPlaying = false
    @State private var timer: Timer? = nil
    
    var body: some View {
        VStack(spacing: 30) {
            HStack {
                Text("Skor: \(score)")
                    .font(.title2.bold())
                    .foregroundColor(.green)
                Spacer()
                Text("Süre: \(timeLeft)")
                    .font(.title2.bold())
                    .foregroundColor(.orange)
            }
            .padding(.horizontal)
            
            if isPlaying {
                Text(question)
                    .font(.system(size: 42, weight: .bold))
                    .foregroundColor(.white)
                    .padding(.vertical, 30)
                
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                    ForEach(options, id: \.self) { num in
                        Button {
                            checkAnswer(num)
                        } label: {
                            Text("\(num)")
                                .font(.title.bold())
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 70)
                                .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                                .cornerRadius(14)
                        }
                    }
                }
                .padding(.horizontal)
            } else {
                VStack(spacing: 20) {
                    Text("Matematik Fırtınası")
                        .font(.title.bold())
                        .foregroundColor(.white)
                    
                    Text("Hızlı hesap yap,\ndoğru cevabı seç!")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    
                    Button {
                        startGame()
                    } label: {
                        Text("BAŞLA")
                            .font(.headline.bold())
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal, 40)
                }
            }
            
            Spacer()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func startGame() {
        score = 0
        timeLeft = 30
        isPlaying = true
        nextQuestion()
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeLeft > 0 {
                timeLeft -= 1
            } else {
                isPlaying = false
                timer?.invalidate()
            }
        }
    }
    
    private func nextQuestion() {
        let a = Int.random(in: 2...12)
        let b = Int.random(in: 2...12)
        let op = ["+", "-", "×"].randomElement()!
        
        switch op {
        case "+":
            correctAnswer = a + b
            question = "\(a) + \(b) = ?"
        case "-":
            correctAnswer = a - b
            question = "\(a) - \(b) = ?"
        default:
            correctAnswer = a * b
            question = "\(a) × \(b) = ?"
        }
        
        var opts = Set<Int>()
        opts.insert(correctAnswer)
        while opts.count < 4 {
            let offset = Int.random(in: -8...8)
            if offset != 0 {
                opts.insert(correctAnswer + offset)
            }
        }
        options = Array(opts).shuffled()
    }
    
    private func checkAnswer(_ selected: Int) {
        if selected == correctAnswer {
            score += 1
        }
        nextQuestion()
    }
}

// ==================== KELİME AVCISI ====================
struct WordHunterView: View {
    @State private var score = 0
    @State private var currentWord = ""
    @State private var scrambled = ""
    @State private var userInput = ""
    @State private var timeLeft = 45
    @State private var isPlaying = false
    @State private var timer: Timer? = nil
    @State private var message = ""
    
    let words = ["ODAK", "DİKKAT", "ZİHİN", "BEYİN", "HAFIZA", "HIZ", "GÜÇ", "BAŞARI", "HEDEF", "PLAN"]
    
    var body: some View {
        VStack(spacing: 25) {
            HStack {
                Text("Skor: \(score)")
                    .font(.title2.bold())
                    .foregroundColor(.green)
                Spacer()
                Text("Süre: \(timeLeft)")
                    .font(.title2.bold())
                    .foregroundColor(.orange)
            }
            .padding(.horizontal)
            
            if isPlaying {
                Text("Harfleri düzenle:")
                    .foregroundColor(.gray)
                
                Text(scrambled)
                    .font(.system(size: 36, weight: .bold, design: .monospaced))
                    .foregroundColor(.green)
                    .padding()
                    .background(Color(red: 0.14, green: 0.15, blue: 0.17))
                    .cornerRadius(12)
                
                TextField("Cevabını yaz...", text: $userInput)
                    .padding()
                    .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                    .cornerRadius(12)
                    .foregroundColor(.white)
                    .autocapitalization(.allCharacters)
                    .padding(.horizontal)
                
                Button {
                    checkWord()
                } label: {
                    Text("KONTROL ET")
                        .font(.headline.bold())
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(14)
                }
                .padding(.horizontal)
                
                if !message.isEmpty {
                    Text(message)
                        .foregroundColor(message.contains("Doğru") ? .green : .red)
                }
            } else {
                VStack(spacing: 20) {
                    Text("Kelime Avcısı")
                        .font(.title.bold())
                        .foregroundColor(.white)
                    
                    Text("Karışık harflerden\nkelimeyi bul!")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    
                    Button {
                        startGame()
                    } label: {
                        Text("BAŞLA")
                            .font(.headline.bold())
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal, 40)
                }
            }
            
            Spacer()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func startGame() {
        score = 0
        timeLeft = 45
        isPlaying = true
        nextWord()
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeLeft > 0 {
                timeLeft -= 1
            } else {
                isPlaying = false
                timer?.invalidate()
            }
        }
    }
    
    private func nextWord() {
        currentWord = words.randomElement()!
        scrambled = String(currentWord.shuffled())
        userInput = ""
        message = ""
    }
    
    private func checkWord() {
        if userInput.uppercased() == currentWord {
            score += 1
            message = "Doğru! +1"
            nextWord()
        } else {
            message = "Yanlış, tekrar dene"
        }
    }
}

// ==================== KÜP TAKİBİ ====================
struct CubeTrackingView: View {
    @State private var score = 0
    @State private var targetCount = 0
    @State private var cubes: [Bool] = []
    @State private var timeLeft = 20
    @State private var isPlaying = false
    @State private var timer: Timer? = nil
    
    var body: some View {
        GeometryReader { geo in
            VStack(spacing: 20) {
                HStack {
                    Text("Skor: \(score)")
                        .font(.title2.bold())
                        .foregroundColor(.green)
                    Spacer()
                    Text("Süre: \(timeLeft)")
                        .font(.title2.bold())
                        .foregroundColor(.orange)
                }
                .padding(.horizontal)
                
                if isPlaying {
                    Text("Kaç tane yeşil küp var?")
                        .foregroundColor(.white)
                        .font(.headline)
                    
                    let availableWidth = geo.size.width - 40
                    let cubeSize = min(availableWidth / 4 - 8, 70)
                    
                    LazyVGrid(
                        columns: Array(repeating: GridItem(.fixed(cubeSize), spacing: 8), count: 4),
                        spacing: 8
                    ) {
                        ForEach(0..<cubes.count, id: \.self) { i in
                            RoundedRectangle(cornerRadius: 8)
                                .fill(cubes[i] ? Color.green : Color.gray.opacity(0.4))
                                .frame(width: cubeSize, height: cubeSize)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.horizontal, 16)
                    
                    let buttonSize = min((availableWidth - 80) / 9, 44)
                    
                    HStack(spacing: 6) {
                        ForEach(0...8, id: \.self) { num in
                            Button {
                                checkCount(num)
                            } label: {
                                Text("\(num)")
                                    .font(.system(size: buttonSize > 36 ? 16 : 14, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(width: buttonSize, height: buttonSize)
                                    .background(Color(red: 0.15, green: 0.16, blue: 0.18))
                                    .cornerRadius(8)
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    
                } else {
                    VStack(spacing: 20) {
                        Text("Küp Takibi")
                            .font(.title.bold())
                            .foregroundColor(.white)
                        
                        Text("Yeşil küpleri hızlıca say!")
                            .foregroundColor(.gray)
                        
                        Button {
                            startGame()
                        } label: {
                            Text("BAŞLA")
                                .font(.headline.bold())
                                .foregroundColor(.black)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.green)
                                .cornerRadius(14)
                        }
                        .padding(.horizontal, 40)
                    }
                    .frame(maxHeight: .infinity)
                }
                
                Spacer()
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func startGame() {
        score = 0
        timeLeft = 20
        isPlaying = true
        nextRound()
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeLeft > 0 {
                timeLeft -= 1
            } else {
                isPlaying = false
                timer?.invalidate()
            }
        }
    }
    
    private func nextRound() {
        targetCount = Int.random(in: 2...6)
        cubes = (0..<12).map { _ in Bool.random() }
        
        var currentGreens = cubes.filter { $0 }.count
        while currentGreens != targetCount {
            if let idx = cubes.indices.randomElement() {
                cubes[idx].toggle()
                currentGreens = cubes.filter { $0 }.count
            }
        }
    }
    
    private func checkCount(_ selected: Int) {
        if selected == targetCount {
            score += 1
        }
        nextRound()
    }
}

// ==================== LABİRENT SERÜVENİ ====================
struct MazeGameView: View {
    @State private var maze: [[Int]] = []
    @State private var playerPos: (row: Int, col: Int) = (1, 1)
    @State private var score = 0
    @State private var level = 1
    @State private var isWon = false
    @State private var message = "Ok tuşlarıyla ilerle"
    
    let size = 11
    
    var body: some View {
        VStack(spacing: 16) {
            HStack {
                Text("Seviye: \(level)")
                    .font(.headline)
                    .foregroundColor(.green)
                Spacer()
                Text("Skor: \(score)")
                    .font(.headline)
                    .foregroundColor(.orange)
            }
            .padding(.horizontal)
            
            Text(message)
                .font(.subheadline)
                .foregroundColor(.gray)
            
            VStack(spacing: 2) {
                ForEach(0..<size, id: \.self) { row in
                    HStack(spacing: 2) {
                        ForEach(0..<size, id: \.self) { col in
                            Rectangle()
                                .fill(cellColor(row: row, col: col))
                                .frame(width: 28, height: 28)
                                .cornerRadius(3)
                        }
                    }
                }
            }
            .padding()
            .background(Color(red: 0.12, green: 0.13, blue: 0.15))
            .cornerRadius(12)
            
            VStack(spacing: 8) {
                Button { move(0, -1) } label: {
                    Image(systemName: "chevron.up")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                        .frame(width: 60, height: 50)
                        .background(Color.green.opacity(0.8))
                        .cornerRadius(10)
                }
                
                HStack(spacing: 20) {
                    Button { move(-1, 0) } label: {
                        Image(systemName: "chevron.left")
                            .font(.title2.bold())
                            .foregroundColor(.white)
                            .frame(width: 60, height: 50)
                            .background(Color.green.opacity(0.8))
                            .cornerRadius(10)
                    }
                    
                    Button { move(1, 0) } label: {
                        Image(systemName: "chevron.right")
                            .font(.title2.bold())
                            .foregroundColor(.white)
                            .frame(width: 60, height: 50)
                            .background(Color.green.opacity(0.8))
                            .cornerRadius(10)
                    }
                }
                
                Button { move(0, 1) } label: {
                    Image(systemName: "chevron.down")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                        .frame(width: 60, height: 50)
                        .background(Color.green.opacity(0.8))
                        .cornerRadius(10)
                }
            }
            
            if isWon {
                Button {
                    nextLevel()
                } label: {
                    Text("SONRAKİ SEVİYE")
                        .font(.headline.bold())
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(14)
                }
                .padding(.horizontal, 40)
            }
            
            Spacer()
        }
        .onAppear {
            generateMaze()
        }
    }
    
    private func cellColor(row: Int, col: Int) -> Color {
        if row == playerPos.row && col == playerPos.col {
            return .yellow
        }
        if maze.isEmpty { return .gray }
        if maze[row][col] == 1 {
            return Color(red: 0.25, green: 0.25, blue: 0.28)
        }
        if row == size-2 && col == size-2 {
            return .green
        }
        return Color(red: 0.15, green: 0.16, blue: 0.18)
    }
    
    private func generateMaze() {
        maze = Array(repeating: Array(repeating: 1, count: size), count: size)
        
        for r in 1..<size-1 {
            for c in 1..<size-1 {
                maze[r][c] = 0
            }
        }
        
        for _ in 0..<(level * 8) {
            let r = Int.random(in: 1..<size-1)
            let c = Int.random(in: 1..<size-1)
            if (r, c) != (1, 1) && (r, c) != (size-2, size-2) {
                maze[r][c] = 1
            }
        }
        
        maze[1][1] = 0
        maze[size-2][size-2] = 0
        
        playerPos = (1, 1)
        isWon = false
        message = "Yeşil noktaya ulaş!"
    }
    
    private func move(_ dx: Int, _ dy: Int) {
        guard !isWon else { return }
        
        let newRow = playerPos.row + dy
        let newCol = playerPos.col + dx
        
        if newRow >= 0 && newRow < size && newCol >= 0 && newCol < size && maze[newRow][newCol] == 0 {
            playerPos = (newRow, newCol)
            
            if newRow == size-2 && newCol == size-2 {
                isWon = true
                score += level * 50
                message = "Tebrikler! Seviye tamamlandı 🎉"
            }
        }
    }
    
    private func nextLevel() {
        level += 1
        generateMaze()
    }
}

// ==================== SUDOKU ====================
struct SudokuGameView: View {
    @State private var board: [[Int]] = Array(repeating: Array(repeating: 0, count: 9), count: 9)
    @State private var original: [[Bool]] = Array(repeating: Array(repeating: false, count: 9), count: 9)
    @State private var selectedRow = -1
    @State private var selectedCol = -1
    @State private var message = "Bir hücre seç ve sayı gir"
    @State private var isComplete = false
    
    private let puzzle: [[Int]] = [
        [5,3,0,0,7,0,0,0,0],
        [6,0,0,1,9,5,0,0,0],
        [0,9,8,0,0,0,0,6,0],
        [8,0,0,0,6,0,0,0,3],
        [4,0,0,8,0,3,0,0,1],
        [7,0,0,0,2,0,0,0,6],
        [0,6,0,0,0,0,2,8,0],
        [0,0,0,4,1,9,0,0,5],
        [0,0,0,0,8,0,0,7,9]
    ]
    
    var body: some View {
        VStack(spacing: 16) {
            Text(message)
                .font(.subheadline)
                .foregroundColor(isComplete ? .green : .gray)
            
            VStack(spacing: 0) {
                ForEach(0..<9, id: \.self) { row in
                    HStack(spacing: 0) {
                        ForEach(0..<9, id: \.self) { col in
                            Button {
                                if !original[row][col] {
                                    selectedRow = row
                                    selectedCol = col
                                }
                            } label: {
                                Text(board[row][col] == 0 ? "" : "\(board[row][col])")
                                    .font(.system(size: 18, weight: original[row][col] ? .bold : .medium))
                                    .foregroundColor(original[row][col] ? .white : .green)
                                    .frame(width: 34, height: 34)
                                    .background(
                                        selectedRow == row && selectedCol == col ?
                                        Color.green.opacity(0.3) :
                                        ((row / 3 + col / 3) % 2 == 0 ?
                                         Color(red: 0.14, green: 0.15, blue: 0.17) :
                                         Color(red: 0.11, green: 0.12, blue: 0.14))
                                    )
                                    .border(Color.gray.opacity(0.4), width: 0.5)
                            }
                        }
                    }
                }
            }
            .cornerRadius(8)
            .padding(.horizontal)
            
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 5), spacing: 10) {
                ForEach(1...9, id: \.self) { num in
                    Button {
                        placeNumber(num)
                    } label: {
                        Text("\(num)")
                            .font(.title2.bold())
                            .foregroundColor(.white)
                            .frame(width: 50, height: 50)
                            .background(Color.green.opacity(0.8))
                            .cornerRadius(10)
                    }
                }
                
                Button {
                    placeNumber(0)
                } label: {
                    Image(systemName: "delete.left")
                        .font(.title3)
                        .foregroundColor(.white)
                        .frame(width: 50, height: 50)
                        .background(Color.red.opacity(0.7))
                        .cornerRadius(10)
                }
            }
            .padding(.horizontal)
            
            Button {
                checkSolution()
            } label: {
                Text("KONTROL ET")
                    .font(.headline.bold())
                    .foregroundColor(.black)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.green)
                    .cornerRadius(14)
            }
            .padding(.horizontal, 40)
            
            Spacer()
        }
        .onAppear {
            loadPuzzle()
        }
    }
    
    private func loadPuzzle() {
        for r in 0..<9 {
            for c in 0..<9 {
                board[r][c] = puzzle[r][c]
                original[r][c] = puzzle[r][c] != 0
            }
        }
    }
    
    private func placeNumber(_ num: Int) {
        guard selectedRow >= 0 && selectedCol >= 0 else { return }
        guard !original[selectedRow][selectedCol] else { return }
        
        board[selectedRow][selectedCol] = num
        message = num == 0 ? "Silindi" : "\(num) yazıldı"
    }
    
    private func checkSolution() {
        for r in 0..<9 {
            for c in 0..<9 {
                if board[r][c] == 0 {
                    message = "Hâlâ boş hücreler var!"
                    return
                }
            }
        }
        
        for r in 0..<9 {
            if Set(board[r]).count != 9 {
                message = "Hata var, tekrar kontrol et"
                return
            }
        }
        
        for c in 0..<9 {
            var colSet = Set<Int>()
            for r in 0..<9 {
                colSet.insert(board[r][c])
            }
            if colSet.count != 9 {
                message = "Hata var, tekrar kontrol et"
                return
            }
        }
        
        isComplete = true
        message = "Tebrikler! Sudoku tamamlandı 🎉"
    }
}

// ==================== OKÇU TERMİNALİ ====================
struct OkcuTerminaliView: View {
    @State private var score = 0
    @State private var timeLeft = 30
    @State private var isPlaying = false
    @State private var timer: Timer? = nil
    @State private var moveTimer: Timer? = nil
    
    @State private var targetX: CGFloat = 0
    @State private var direction: CGFloat = 1
    @State private var speed: CGFloat = 4
    @State private var lastHitMessage = ""
    
    private let trackWidth: CGFloat = 280
    private let perfectZone: CGFloat = 36
    
    var body: some View {
        VStack(spacing: 28) {
            HStack {
                Text("Skor: \(score)")
                    .font(.title2.bold())
                    .foregroundColor(.green)
                Spacer()
                Text("Süre: \(timeLeft)")
                    .font(.title2.bold())
                    .foregroundColor(.orange)
            }
            .padding(.horizontal)
            
            if isPlaying {
                Text("Ortadaki yeşil çizgiye denk getir!")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(red: 0.14, green: 0.15, blue: 0.17))
                        .frame(width: trackWidth + 40, height: 50)
                    
                    Rectangle()
                        .fill(Color.green.opacity(0.35))
                        .frame(width: perfectZone, height: 60)
                    
                    Rectangle()
                        .fill(Color.green)
                        .frame(width: 3, height: 70)
                    
                    Circle()
                        .fill(Color.red)
                        .frame(width: 28, height: 28)
                        .overlay(Circle().stroke(Color.white, lineWidth: 2))
                        .offset(x: targetX)
                }
                .frame(height: 80)
                
                if !lastHitMessage.isEmpty {
                    Text(lastHitMessage)
                        .font(.headline)
                        .foregroundColor(lastHitMessage.contains("İsabet") ? .green : .red)
                        .transition(.opacity)
                }
                
                Button {
                    fire()
                } label: {
                    Text("ATEŞ ET")
                        .font(.headline.bold())
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 18)
                        .background(Color.green)
                        .cornerRadius(14)
                }
                .padding(.horizontal, 40)
                
            } else {
                VStack(spacing: 20) {
                    Text("Okçu Terminali")
                        .font(.title.bold())
                        .foregroundColor(.white)
                    
                    Text("Hedefi ortadaki yeşil çizgiye\ndenkle ve ateş et!\nZamanlama önemli.")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    
                    Button {
                        startGame()
                    } label: {
                        Text("BAŞLA")
                            .font(.headline.bold())
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal, 40)
                }
            }
            
            Spacer()
        }
        .onDisappear {
            timer?.invalidate()
            moveTimer?.invalidate()
        }
    }
    
    private func startGame() {
        score = 0
        timeLeft = 30
        isPlaying = true
        lastHitMessage = ""
        targetX = 0
        direction = 1
        speed = 4
        
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeLeft > 0 {
                timeLeft -= 1
            } else {
                endGame()
            }
        }
        
        moveTimer = Timer.scheduledTimer(withTimeInterval: 0.02, repeats: true) { _ in
            targetX += speed * direction
            
            if abs(targetX) > trackWidth / 2 {
                direction *= -1
                targetX = max(min(targetX, trackWidth / 2), -trackWidth / 2)
            }
        }
    }
    
    private func fire() {
        let distance = abs(targetX)
        
        withAnimation(.easeOut(duration: 0.2)) {
            if distance <= perfectZone / 2 {
                let points = distance <= 8 ? 10 : 5
                score += points
                lastHitMessage = points == 10 ? "Mükemmel İsabet! +\(points)" : "İsabet! +\(points)"
                speed = min(speed + 0.3, 9)
            } else {
                lastHitMessage = "Iska!"
            }
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            withAnimation {
                lastHitMessage = ""
            }
        }
    }
    
    private func endGame() {
        isPlaying = false
        timer?.invalidate()
        moveTimer?.invalidate()
        lastHitMessage = "Süre bitti! Toplam: \(score)"
    }
}

// ==================== GÖRSEL YAPBOZ ====================
struct GorselYapbozView: View {
    @State private var tiles: [Int] = []
    @State private var isPlaying = false
    @State private var isWon = false
    @State private var moves = 0
    @State private var timeLeft = 60
    @State private var timer: Timer? = nil
    
    private let size = 3
    private let correct: [Int] = [1, 2, 3, 4, 5, 6, 7, 8, 0]
    
    var body: some View {
        VStack(spacing: 24) {
            HStack {
                Text("Hamle: \(moves)")
                    .font(.title3.bold())
                    .foregroundColor(.green)
                Spacer()
                Text("Süre: \(timeLeft)")
                    .font(.title3.bold())
                    .foregroundColor(.orange)
            }
            .padding(.horizontal)
            
            if isPlaying {
                Text(isWon ? "Tebrikler! Tamamladın 🎉" : "Kareleri kaydırarak sırala")
                    .font(.subheadline)
                    .foregroundColor(isWon ? .green : .gray)
                
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: size), spacing: 6) {
                    ForEach(0..<tiles.count, id: \.self) { index in
                        Button {
                            moveTile(at: index)
                        } label: {
                            ZStack {
                                RoundedRectangle(cornerRadius: 10)
                                    .fill(tiles[index] == 0 ? Color.clear : Color(red: 0.18, green: 0.22, blue: 0.28))
                                    .frame(height: 80)
                                
                                if tiles[index] != 0 {
                                    Text("\(tiles[index])")
                                        .font(.title.bold())
                                        .foregroundColor(.white)
                                }
                            }
                        }
                        .disabled(isWon || tiles[index] == 0)
                    }
                }
                .padding(.horizontal, 30)
                
                if isWon {
                    Button {
                        startGame()
                    } label: {
                        Text("YENİDEN OYNA")
                            .font(.headline.bold())
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal, 40)
                    .padding(.top, 10)
                }
            } else {
                VStack(spacing: 20) {
                    Text("Görsel Yapboz")
                        .font(.title.bold())
                        .foregroundColor(.white)
                    
                    Text("3x3 kaydırmalı yapboz.\nKareleri kaydırarak 1-8 sırasına getir.")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.gray)
                    
                    Button {
                        startGame()
                    } label: {
                        Text("BAŞLA")
                            .font(.headline.bold())
                            .foregroundColor(.black)
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.green)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal, 40)
                }
            }
            
            Spacer()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func startGame() {
        moves = 0
        timeLeft = 60
        isWon = false
        isPlaying = true
        
        tiles = correct
        for _ in 0..<100 {
            let emptyIndex = tiles.firstIndex(of: 0)!
            let neighbors = getNeighbors(of: emptyIndex)
            if let randomNeighbor = neighbors.randomElement() {
                tiles.swapAt(emptyIndex, randomNeighbor)
            }
        }
        
        if tiles == correct {
            startGame()
            return
        }
        
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeLeft > 0 && !isWon {
                timeLeft -= 1
            } else if timeLeft == 0 {
                isPlaying = false
                timer?.invalidate()
            }
        }
    }
    
    private func moveTile(at index: Int) {
        guard !isWon else { return }
        
        let emptyIndex = tiles.firstIndex(of: 0)!
        let neighbors = getNeighbors(of: emptyIndex)
        
        if neighbors.contains(index) {
            withAnimation(.easeInOut(duration: 0.15)) {
                tiles.swapAt(index, emptyIndex)
            }
            moves += 1
            
            if tiles == correct {
                isWon = true
                timer?.invalidate()
            }
        }
    }
    
    private func getNeighbors(of index: Int) -> [Int] {
        var neighbors: [Int] = []
        let row = index / size
        let col = index % size
        
        if row > 0 { neighbors.append(index - size) }
        if row < size - 1 { neighbors.append(index + size) }
        if col > 0 { neighbors.append(index - 1) }
        if col < size - 1 { neighbors.append(index + 1) }
        
        return neighbors
    }
}

// ==================== GENEL OYUN ====================
struct GenericGameView: View {
    let title: String
    let subtitle: String
    @State private var isStarted = false
    @State private var score = 0
    
    var body: some View {
        VStack(spacing: 30) {
            Spacer()
            
            Image(systemName: "gamecontroller.fill")
                .font(.system(size: 60))
                .foregroundColor(.green)
            
            Text(title)
                .font(.title.bold())
                .foregroundColor(.white)
            
            Text(subtitle)
                .font(.subheadline)
                .foregroundColor(.gray)
                .multilineTextAlignment(.center)
            
            if isStarted {
                Text("Skor: \(score)")
                    .font(.largeTitle.bold())
                    .foregroundColor(.green)
                    .padding(.top, 20)
                
                Button {
                    score += Int.random(in: 5...15)
                } label: {
                    Text("PUAN KAZAN")
                        .font(.headline.bold())
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(14)
                }
                .padding(.horizontal, 40)
            } else {
                Button {
                    isStarted = true
                } label: {
                    Text("OYUNU BAŞLAT")
                        .font(.headline.bold())
                        .foregroundColor(.black)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green)
                        .cornerRadius(14)
                }
                .padding(.horizontal, 40)
            }
            
            Spacer()
        }
        .padding()
    }
}

// MARK: - TAKVİM VIEW (BÜYÜTÜLMÜŞ PANEL)
struct TakvimView: View {
    @Binding var showSettings: Bool
    @Binding var showAuth: Bool
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var selectedDay = 9
    @State private var selectedDuration: Int = 25
    @State private var customDuration: Double = 25
    @State private var timeRemaining: Int = 25 * 60
    @State private var isRunning = false
    @State private var timer: Timer? = nil
    @State private var showAlert = false
    @State private var alertMessage = ""
    
    let days = [
        (name: "WED", number: 7),
        (name: "THU", number: 8),
        (name: "FRI", number: 9),
        (name: "SAT", number: 10),
        (name: "SUN", number: 11),
        (name: "MON", number: 12),
        (name: "TUE", number: 13)
    ]
    
    var leaderboard: [LeaderboardUser] {
        var board: [LeaderboardUser] = [
            LeaderboardUser(rank: 1, name: "AI FOCUS COACH", title: "Kurucu Ortak", xp: 7416, isGradient: true),
            LeaderboardUser(rank: 2, name: "ELIF KAYA", title: "Kurucu Ortak", xp: 5860),
            LeaderboardUser(rank: 3, name: "ZEYNEP DEMIR", title: "Kurucu Ortak", xp: 4788),
            LeaderboardUser(rank: 4, name: "AHMET YILMAZ", title: "Kurucu Ortak", xp: 4100),
            LeaderboardUser(rank: 5, name: "BURAK ÇELIK", title: "Kurucu Ortak", xp: 4097),
            LeaderboardUser(rank: 6, name: "CAN ORUÇ", title: "Kurucu Ortak", xp: 3561),
            LeaderboardUser(rank: 7, name: "YİĞİT BAY EFE", title: "Girişimci", xp: 1094, initial: "Y"),
            LeaderboardUser(rank: 8, name: "YİGİTTUSEM2008", title: "Girişimci", xp: 855, initial: "Y")
        ]
        
        if isLoggedIn {
            let username = userEmail.components(separatedBy: "@").first?.uppercased() ?? userEmail.uppercased()
            board.append(
                LeaderboardUser(
                    rank: 9,
                    name: username,
                    title: "Stajyer (Garaj)",
                    xp: 5,
                    isMe: true
                )
            )
        } else {
            board.append(
                LeaderboardUser(
                    rank: 9,
                    name: "MESUTBAY03",
                    title: "Stajyer (Garaj)",
                    xp: 5,
                    isMe: true
                )
            )
        }
        
        return board
    }
    
    var progress: CGFloat {
        let total = max(selectedDuration * 60, 1)
        return CGFloat(timeRemaining) / CGFloat(total)
    }
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 16) {
                    HStack {
                        Text(isLoggedIn ? "Bağlı: \(userEmail)" : "Bağlı değil")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Spacer()
                        
                        Button { showSettings = true } label: {
                            Image(systemName: "gearshape")
                                .foregroundColor(.white)
                        }
                        
                        Button {
                            showAuth = true
                        } label: {
                            Circle()
                                .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 28, height: 28)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 8)
                    
                    VStack(spacing: 20) {
                        Text("OCTOBER 2026")
                            .font(.title2.weight(.bold))
                            .foregroundColor(.green)
                        
                        HStack(spacing: 12) {
                            ForEach(days, id: \.number) { day in
                                Button {
                                    selectedDay = day.number
                                } label: {
                                    VStack(spacing: 8) {
                                        Text(day.name)
                                            .font(.caption)
                                            .foregroundColor(.gray)
                                        
                                        Text("\(day.number)")
                                            .font(.title2.weight(.bold))
                                            .foregroundColor(selectedDay == day.number ? .green : .white)
                                            .frame(width: 52, height: 52)
                                            .background(
                                                selectedDay == day.number ?
                                                Color.green.opacity(0.2) : Color.clear
                                            )
                                            .overlay(
                                                Circle()
                                                    .stroke(selectedDay == day.number ? Color.green : Color.clear, lineWidth: 2.5)
                                            )
                                    }
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 28)
                    .padding(.horizontal, 16)
                    .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                    .cornerRadius(20)
                    .padding(.horizontal)
                    
                    VStack(spacing: 16) {
                        HStack {
                            Text("FOCUS TIMER")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            Image(systemName: "cloud.rain")
                                .foregroundColor(.gray)
                            Image(systemName: "flame.fill")
                                .foregroundColor(.orange)
                            
                            Spacer()
                            
                            HStack(spacing: 12) {
                                DurationButton(title: "25m", isSelected: selectedDuration == 25) {
                                    setDuration(25)
                                }
                                DurationButton(title: "45m", isSelected: selectedDuration == 45) {
                                    setDuration(45)
                                }
                                DurationButton(title: "60m", isSelected: selectedDuration == 60) {
                                    setDuration(60)
                                }
                            }
                        }
                        
                        HStack {
                            Text("Özel Süre:")
                                .font(.subheadline)
                                .foregroundColor(.gray)
                            Spacer()
                            Text("\(Int(customDuration)) dk")
                                .font(.subheadline.weight(.semibold))
                                .foregroundColor(.white)
                        }
                        
                        Slider(value: $customDuration, in: 5...90, step: 5)
                            .accentColor(.green)
                            .onChange(of: customDuration) { newValue in
                                if !isRunning {
                                    selectedDuration = Int(newValue)
                                    timeRemaining = selectedDuration * 60
                                }
                            }
                        
                        ZStack {
                            Circle()
                                .stroke(Color.gray.opacity(0.3), lineWidth: 12)
                                .frame(width: 200, height: 200)
                            
                            Circle()
                                .trim(from: 0, to: progress)
                                .stroke(
                                    AngularGradient(colors: [.green, .yellow], center: .center),
                                    style: StrokeStyle(lineWidth: 12, lineCap: .round)
                                )
                                .frame(width: 200, height: 200)
                                .rotationEffect(.degrees(-90))
                                .animation(.linear(duration: 1), value: timeRemaining)
                            
                            Text(timeString(from: timeRemaining))
                                .font(.system(size: 42, weight: .bold, design: .rounded))
                                .foregroundColor(.white)
                        }
                        .padding(.vertical, 10)
                        
                        HStack(spacing: 20) {
                            Button {
                                if isRunning {
                                    stopTimer()
                                } else {
                                    startTimer()
                                }
                            } label: {
                                Text(isRunning ? "DURDUR" : "BAŞLAT")
                                    .font(.headline.weight(.bold))
                                    .foregroundColor(.black)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 16)
                                    .background(Color.green)
                                    .cornerRadius(14)
                            }
                            
                            Button {
                                resetTimer()
                            } label: {
                                Image(systemName: "arrow.counterclockwise")
                                    .font(.title2)
                                    .foregroundColor(.white)
                                    .padding(14)
                                    .background(Color(red: 0.18, green: 0.19, blue: 0.21))
                                    .clipShape(Circle())
                            }
                        }
                    }
                    .padding()
                    .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                    .cornerRadius(16)
                    .padding(.horizontal)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("LİDERLİK TABLOSU")
                                .font(.headline)
                                .foregroundColor(.green)
                            Spacer()
                            Button {
                                showMessage("Liderlik tablosu yenilendi")
                            } label: {
                                Image(systemName: "arrow.clockwise")
                                    .foregroundColor(.gray)
                            }
                        }
                        
                        HStack {
                            Text("#")
                                .frame(width: 30, alignment: .leading)
                            Text("USER")
                            Spacer()
                            Text("RANK")
                                .frame(width: 100, alignment: .leading)
                            Text("XP")
                                .frame(width: 50, alignment: .trailing)
                        }
                        .font(.caption)
                        .foregroundColor(.gray)
                        .padding(.horizontal, 4)
                        
                        ForEach(leaderboard) { user in
                            HStack(spacing: 10) {
                                Text("\(user.rank)")
                                    .font(.subheadline.weight(.bold))
                                    .foregroundColor(.white)
                                    .frame(width: 24)
                                
                                if user.isGradient {
                                    Circle()
                                        .fill(LinearGradient(colors: [.purple, .blue, .cyan], startPoint: .topLeading, endPoint: .bottomTrailing))
                                        .frame(width: 36, height: 36)
                                } else if let initial = user.initial {
                                    Circle()
                                        .fill(Color.purple)
                                        .frame(width: 36, height: 36)
                                        .overlay(Text(initial).foregroundColor(.white).fontWeight(.bold))
                                } else if user.isMe {
                                    Circle()
                                        .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                                        .frame(width: 36, height: 36)
                                } else {
                                    Circle()
                                        .fill(Color.gray.opacity(0.4))
                                        .frame(width: 36, height: 36)
                                        .overlay(Image(systemName: "person.fill").foregroundColor(.white).font(.caption))
                                }
                                
                                Text(user.name)
                                    .font(.subheadline.weight(.medium))
                                    .foregroundColor(user.isMe ? .green : .white)
                                    .lineLimit(1)
                                
                                Spacer()
                                
                                Text(user.title)
                                    .font(.caption)
                                    .foregroundColor(.gray)
                                    .frame(width: 100, alignment: .leading)
                                
                                Text("\(user.xp)")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundColor(.green)
                                    .frame(width: 50, alignment: .trailing)
                            }
                            .padding(.vertical, 6)
                            .padding(.horizontal, 4)
                            .background(user.isMe ? Color.green.opacity(0.1) : Color.clear)
                            .cornerRadius(8)
                        }
                    }
                    .padding()
                    .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                    .cornerRadius(16)
                    .padding(.horizontal)
                    
                    Spacer().frame(height: 120)
                }
            }
        }
        .alert(alertMessage, isPresented: $showAlert) {
            Button("Tamam", role: .cancel) { }
        }
        .onDisappear {
            stopTimer()
        }
    }
    
    private func setDuration(_ minutes: Int) {
        guard !isRunning else { return }
        selectedDuration = minutes
        customDuration = Double(minutes)
        timeRemaining = minutes * 60
    }
    
    private func startTimer() {
        isRunning = true
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeRemaining > 0 {
                timeRemaining -= 1
            } else {
                stopTimer()
                showMessage("Süre doldu! Tebrikler 🎉")
            }
        }
    }
    
    private func stopTimer() {
        isRunning = false
        timer?.invalidate()
        timer = nil
    }
    
    private func resetTimer() {
        stopTimer()
        timeRemaining = selectedDuration * 60
    }
    
    private func timeString(from seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }
    
    private func showMessage(_ msg: String) {
        alertMessage = msg
        showAlert = true
    }
}

struct DurationButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(.semibold))
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(isSelected ? Color.green.opacity(0.25) : Color(red: 0.18, green: 0.20, blue: 0.22))
                .foregroundColor(isSelected ? .green : .white)
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(isSelected ? Color.green : Color.clear, lineWidth: 1.5)
                )
        }
    }
}

struct LeaderboardUser: Identifiable {
    let id = UUID()
    let rank: Int
    let name: String
    let title: String
    let xp: Int
    var isGradient: Bool = false
    var initial: String? = nil
    var isMe: Bool = false
}

// MARK: - OFİS VIEW (İŞLEVSEL)
struct OfisView: View {
    @Binding var showSettings: Bool
    @Binding var showAuth: Bool
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var showAlert = false
    @State private var alertMessage = ""
    @State private var showLibrarySelection = false
    @State private var showLibrarySession = false
    @State private var selectedDuration = 25
    
    @AppStorage("coins") private var coins = 225
    @AppStorage("officeLevel") private var officeLevel = 3
    @AppStorage("desks") private var desks = 3
    @AppStorage("employees") private var employees = 3
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            VStack(spacing: 0) {
                HStack {
                    Text("Sanal Ofis")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    HStack(spacing: 18) {
                        Button { showMessage("Arama") } label: { Image(systemName: "magnifyingglass") }
                        Button { showMessage("Bildirimler") } label: { Image(systemName: "bell") }
                        Button { showSettings = true } label: { Image(systemName: "gearshape") }
                        
                        Button {
                            showAuth = true
                        } label: {
                            Circle()
                                .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 28, height: 28)
                        }
                    }
                    .foregroundColor(.white)
                    .font(.system(size: 18))
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 12)
                
                HStack(spacing: 0) {
                    ForEach(["GÖREV", "GELEN", "BAŞARIM", "NOTLAR"], id: \.self) { tab in
                        Text(tab)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(tab == "GÖREV" ? .green : .gray)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                }
                .padding(.horizontal)
                
                HStack {
                    Text(isLoggedIn ? "Bağlı: \(userEmail)" : "Bağlı değil")
                        .font(.caption)
                        .foregroundColor(.gray)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 6)
                
                ScrollView {
                    VStack(spacing: 16) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("YİME CENTER")
                                    .font(.title3.weight(.bold))
                                    .foregroundColor(.green)
                                Text("LVL \(officeLevel) · \(coins) 🟡")
                                    .font(.subheadline)
                                    .foregroundColor(.white)
                            }
                            
                            Spacer()
                            
                            HStack(spacing: 16) {
                                Button { showMessage("Arkadaşlar") } label: {
                                    Image(systemName: "person.2.fill")
                                        .foregroundColor(.gray)
                                }
                                Button { showMessage("Yenile") } label: {
                                    Image(systemName: "arrow.clockwise")
                                        .foregroundColor(.red.opacity(0.8))
                                }
                            }
                        }
                        .padding(.horizontal)
                        
                        ZStack {
                            Color.black.opacity(0.4)
                            
                            HStack(spacing: 30) {
                                DeskView(name: "AHMET YILMAZ", color: .brown)
                                DeskView(name: "ELIF KAYA", color: .brown)
                                DeskView(name: "BURAK ÇELIK", color: .brown)
                            }
                            .padding(.top, 40)
                        }
                        .frame(height: 220)
                        .cornerRadius(16)
                        .padding(.horizontal)
                        
                        VStack(alignment: .leading, spacing: 14) {
                            HStack {
                                Text("PERFORMANS ANALİZİ")
                                    .font(.headline)
                                    .foregroundColor(.green)
                                Spacer()
                                Image(systemName: "chart.bar.fill")
                                    .foregroundColor(.gray)
                            }
                            
                            HStack(spacing: 0) {
                                PerformanceItem(icon: "timer", value: "0", label: "Odak", color: .yellow)
                                PerformanceItem(icon: "circle.dashed", value: "0/2", label: "Görev", color: .white)
                                PerformanceItem(icon: "flame.fill", value: "5", label: "Seri", color: .orange)
                            }
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(16)
                        .padding(.horizontal)
                        
                        VStack(alignment: .leading, spacing: 12) {
                            Text("AÇILAN EŞYALAR")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            HStack(spacing: 10) {
                                ItemSlot(icon: "chair.fill", progress: "3/9", isUnlocked: true)
                                ItemSlot(icon: "laptopcomputer", isUnlocked: true)
                                ItemSlot(icon: "leaf.fill", isUnlocked: true)
                                ItemSlot(icon: "square.split.2x1", isUnlocked: true)
                                ItemSlot(icon: "lock.fill", isUnlocked: officeLevel >= 4)
                                ItemSlot(icon: "lock.fill", isUnlocked: officeLevel >= 5)
                                ItemSlot(icon: "lock.fill", isUnlocked: officeLevel >= 6)
                            }
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(16)
                        .padding(.horizontal)
                        
                        VStack(alignment: .leading, spacing: 12) {
                            Text("OFİS YÜKSELTMELERİ")
                                .font(.headline)
                                .foregroundColor(.white)
                            
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Cost")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                    Text("1500 🟡")
                                        .font(.title3.weight(.bold))
                                        .foregroundColor(.yellow)
                                }
                                
                                Spacer()
                                
                                Button {
                                    let cost = 1500
                                    if coins >= cost {
                                        coins -= cost
                                        officeLevel += 1
                                        desks += 1
                                        employees += 1
                                        showMessage("Ofis yükseltildi!\nYeni seviye: \(officeLevel)")
                                    } else {
                                        showMessage("Yetersiz jeton!\n\(cost - coins) jeton daha gerekli.")
                                    }
                                } label: {
                                    Text("YÜKSELT")
                                        .font(.subheadline.weight(.bold))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 20)
                                        .padding(.vertical, 10)
                                        .background(coins >= 1500 ? Color.green : Color.gray.opacity(0.3))
                                        .cornerRadius(20)
                                }
                            }
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(16)
                        .padding(.horizontal)
                        
                        HStack(spacing: 0) {
                            StatMini(value: "\(desks)", label: "Masa")
                            StatMini(value: "\(employees)", label: "Çalışan")
                            StatMini(value: "\(officeLevel)", label: "Seviye")
                            StatMini(value: "\(coins)", label: "Coins", isCoin: true)
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(16)
                        .padding(.horizontal)
                        
                        Button {
                            showLibrarySelection = true
                        } label: {
                            Text("AKTİF OTURUMA KATIL")
                                .font(.headline.weight(.bold))
                                .foregroundColor(.black)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 18)
                                .background(Color.green)
                                .cornerRadius(14)
                        }
                        .padding(.horizontal)
                        .padding(.bottom, 120)
                    }
                    .padding(.top, 12)
                }
            }
        }
        .alert(alertMessage, isPresented: $showAlert) {
            Button("Tamam", role: .cancel) { }
        }
        .sheet(isPresented: $showLibrarySelection) {
            SanalKutuphaneView(
                selectedDuration: $selectedDuration,
                showSession: $showLibrarySession
            )
        }
        .fullScreenCover(isPresented: $showLibrarySession) {
            KutuphaneMerkeziView(duration: selectedDuration)
        }
    }
    
    private func showMessage(_ msg: String) {
        alertMessage = msg
        showAlert = true
    }
}

// ==================== SANAL KÜTÜPHANE ====================
struct SanalKutuphaneView: View {
    @Environment(\.dismiss) var dismiss
    @Binding var selectedDuration: Int
    @Binding var showSession: Bool
    @State private var isStrictMode = true
    
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.12, green: 0.10, blue: 0.08),
                    Color(red: 0.08, green: 0.07, blue: 0.06)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            VStack(spacing: 30) {
                Spacer()
                
                Image(systemName: "book.closed.fill")
                    .font(.system(size: 60))
                    .foregroundColor(.green)
                
                Text("Sanal Kütüphane")
                    .font(.title.bold())
                    .foregroundColor(.white)
                
                Text("Dünya çapındaki üyelerle sessizce odaklan.")
                    .font(.subheadline)
                    .foregroundColor(.gray)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
                
                HStack {
                    Image(systemName: "lock.fill")
                        .foregroundColor(.green)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("SERT MOD")
                            .font(.subheadline.weight(.bold))
                            .foregroundColor(.white)
                        Text("Otomatik DND (Rahatsız Etmeyin) aktif")
                            .font(.caption)
                            .foregroundColor(.gray)
                    }
                    Spacer()
                    Toggle("", isOn: $isStrictMode)
                        .labelsHidden()
                        .tint(.green)
                }
                .padding()
                .background(Color.white.opacity(0.08))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.green.opacity(0.4), lineWidth: 1)
                )
                .cornerRadius(12)
                .padding(.horizontal, 30)
                
                HStack(spacing: 16) {
                    DurationSelectButton(title: "25m") {
                        selectedDuration = 25
                        dismiss()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showSession = true
                        }
                    }
                    DurationSelectButton(title: "45m") {
                        selectedDuration = 45
                        dismiss()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showSession = true
                        }
                    }
                    DurationSelectButton(title: "60m") {
                        selectedDuration = 60
                        dismiss()
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                            showSession = true
                        }
                    }
                }
                
                Button {
                    dismiss()
                } label: {
                    HStack {
                        Image(systemName: "arrow.left")
                        Text("GERİ DÖN")
                    }
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.gray)
                }
                .padding(.top, 20)
                
                Spacer()
            }
        }
    }
}

struct DurationSelectButton: View {
    let title: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.headline.weight(.bold))
                .foregroundColor(.black)
                .frame(width: 80, height: 44)
                .background(Color.green)
                .cornerRadius(22)
        }
    }
}

// ==================== KÜTÜPHANE MERKEZİ ====================
struct KutuphaneMerkeziView: View {
    @Environment(\.dismiss) var dismiss
    let duration: Int
    
    @State private var timeRemaining: Int
    @State private var timer: Timer? = nil
    
    init(duration: Int) {
        self.duration = duration
        self._timeRemaining = State(initialValue: duration * 60)
    }
    
    let users: [(String, Bool)] = [
        ("MESUTBAY03", true),
        ("AI FOCUS COACH", false),
        ("ELIF KAYA", false),
        ("ZEYNEP DEMIR", false),
        ("BURAK ÇELIK", false),
        ("AHMET YILMAZ", false),
        ("CAN ORUÇ", false),
        ("YİĞİT BAY EFE", false),
        ("YIGITTUSEM2008", false)
    ]
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.9).ignoresSafeArea()
            
            VStack(spacing: 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("KÜTÜPHANE MERKEZİ")
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white)
                        HStack(spacing: 6) {
                            Text("DERİN ÇALIŞMA OTURUMU")
                                .font(.caption)
                                .foregroundColor(.gray)
                            Text("STRICT")
                                .font(.caption2.weight(.bold))
                                .foregroundColor(.black)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.red)
                                .cornerRadius(4)
                        }
                    }
                    
                    Spacer()
                    
                    Text(timeString(from: timeRemaining))
                        .font(.title3.weight(.bold))
                        .foregroundColor(.green)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.green.opacity(0.15))
                        .cornerRadius(8)
                }
                .padding()
                
                ScrollView {
                    VStack(spacing: 10) {
                        ForEach(users, id: \.0) { user in
                            HStack {
                                Circle()
                                    .fill(user.1 ?
                                          LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing) :
                                          LinearGradient(colors: [.gray, .gray.opacity(0.6)], startPoint: .topLeading, endPoint: .bottomTrailing)
                                    )
                                    .frame(width: 40, height: 40)
                                    .overlay(
                                        Text(String(user.0.prefix(1)))
                                            .font(.headline.weight(.bold))
                                            .foregroundColor(.white)
                                    )
                                
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(user.0)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundColor(.white)
                                    Text("DURUM: DERİN_ODAK")
                                        .font(.caption2)
                                        .foregroundColor(.green)
                                }
                                
                                Spacer()
                                
                                if !user.1 {
                                    Image(systemName: "bolt.fill")
                                        .foregroundColor(.yellow)
                                        .font(.caption)
                                }
                            }
                            .padding()
                            .background(Color.white.opacity(0.06))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(user.1 ? Color.green.opacity(0.5) : Color.clear, lineWidth: 1)
                            )
                            .cornerRadius(12)
                        }
                    }
                    .padding(.horizontal)
                }
                
                Button {
                    timer?.invalidate()
                    dismiss()
                } label: {
                    HStack {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                        Text("KÜTÜPHANEDEN AYRIL")
                            .fontWeight(.bold)
                    }
                    .font(.headline)
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(Color.red.opacity(0.85))
                    .cornerRadius(14)
                }
                .padding()
            }
        }
        .onAppear {
            startTimer()
        }
        .onDisappear {
            timer?.invalidate()
        }
    }
    
    private func startTimer() {
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { _ in
            if timeRemaining > 0 {
                timeRemaining -= 1
            } else {
                timer?.invalidate()
                dismiss()
            }
        }
    }
    
    private func timeString(from seconds: Int) -> String {
        let m = seconds / 60
        let s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }
}

struct DeskView: View {
    let name: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            RoundedRectangle(cornerRadius: 6)
                .fill(color.opacity(0.8))
                .frame(width: 80, height: 36)
                .overlay(
                    Text(name)
                        .font(.system(size: 8, weight: .bold))
                        .foregroundColor(.white)
                        .lineLimit(1)
                )
            
            Circle()
                .fill(Color.gray.opacity(0.6))
                .frame(width: 28, height: 28)
                .overlay(
                    Image(systemName: "person.fill")
                        .font(.caption2)
                        .foregroundColor(.white)
                )
            
            Capsule()
                .fill(Color.gray.opacity(0.5))
                .frame(width: 24, height: 10)
        }
    }
}

struct PerformanceItem: View {
    let icon: String
    let value: String
    let label: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.3), lineWidth: 4)
                    .frame(width: 50, height: 50)
                
                Image(systemName: icon)
                    .foregroundColor(color)
            }
            
            Text(value)
                .font(.headline.weight(.bold))
                .foregroundColor(.white)
            
            Text(label)
                .font(.caption2)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
    }
}

struct ItemSlot: View {
    let icon: String
    var progress: String? = nil
    let isUnlocked: Bool
    
    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color(red: 0.16, green: 0.17, blue: 0.19))
                .frame(width: 44, height: 44)
            
            if isUnlocked {
                Image(systemName: icon)
                    .foregroundColor(.green)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundColor(.gray)
            }
            
            if let progress = progress {
                Text(progress)
                    .font(.system(size: 8))
                    .foregroundColor(.white)
                    .offset(y: 18)
            }
        }
    }
}

struct StatMini: View {
    let value: String
    let label: String
    var isCoin: Bool = false
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title3.weight(.bold))
                .foregroundColor(isCoin ? .yellow : .white)
            Text(label)
                .font(.caption2)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - SU VIEW (HATIRLATICI İŞLEVSEL)
struct SuView: View {
    @Binding var showSettings: Bool
    @Binding var showAuth: Bool
    @Binding var isLoggedIn: Bool
    @Binding var userEmail: String
    
    @State private var glassesDrunk: Int = 0
    @AppStorage("waterReminderInterval") private var reminderInterval: Double = 2.0
    @AppStorage("waterReminderEnabled") private var isReminderActive: Bool = false
    @State private var showAlert = false
    @State private var alertMessage = ""
    
    let dailyGoal = 8
    
    var progress: CGFloat {
        CGFloat(glassesDrunk) / CGFloat(dailyGoal)
    }
    
    var body: some View {
        ZStack {
            Color(red: 0.08, green: 0.09, blue: 0.10).ignoresSafeArea()
            
            VStack(spacing: 0) {
                HStack {
                    Text("Su Hatırlatıcı")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                    
                    Spacer()
                    
                    HStack(spacing: 18) {
                        Button { showMessage("Arama") } label: { Image(systemName: "magnifyingglass") }
                        Button { showMessage("Bildirimler") } label: { Image(systemName: "bell") }
                        Button { showSettings = true } label: { Image(systemName: "gearshape") }
                        
                        Button {
                            showAuth = true
                        } label: {
                            Circle()
                                .fill(LinearGradient(colors: [.green, .mint], startPoint: .topLeading, endPoint: .bottomTrailing))
                                .frame(width: 28, height: 28)
                        }
                    }
                    .foregroundColor(.white)
                    .font(.system(size: 18))
                }
                .padding(.horizontal)
                .padding(.top, 8)
                .padding(.bottom, 12)
                
                HStack(spacing: 0) {
                    ForEach(["GÖREV", "GELEN", "BAŞARIM", "NOTLAR"], id: \.self) { tab in
                        Text(tab)
                            .font(.caption.weight(.semibold))
                            .foregroundColor(tab == "GÖREV" ? .green : .gray)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                }
                .padding(.horizontal)
                
                HStack {
                    Text(isLoggedIn ? "Bağlı: \(userEmail)" : "Bağlı değil")
                        .font(.caption)
                        .foregroundColor(.gray)
                    Spacer()
                }
                .padding(.horizontal)
                .padding(.top, 8)
                
                ScrollView {
                    VStack(spacing: 28) {
                        Text("SU HATIRLATICI")
                            .font(.title3.weight(.bold))
                            .foregroundColor(.green)
                            .padding(.top, 20)
                        
                        ZStack {
                            Circle()
                                .stroke(Color.blue.opacity(0.3), lineWidth: 10)
                                .frame(width: 200, height: 200)
                            
                            Circle()
                                .trim(from: 0, to: progress)
                                .stroke(
                                    LinearGradient(colors: [.blue, .cyan], startPoint: .topLeading, endPoint: .bottomTrailing),
                                    style: StrokeStyle(lineWidth: 10, lineCap: .round)
                                )
                                .frame(width: 200, height: 200)
                                .rotationEffect(.degrees(-90))
                                .animation(.easeInOut, value: glassesDrunk)
                            
                            VStack(spacing: 8) {
                                Image(systemName: "drop.fill")
                                    .font(.system(size: 40))
                                    .foregroundColor(.white)
                                
                                Text("\(glassesDrunk) / \(dailyGoal)")
                                    .font(.title.weight(.bold))
                                    .foregroundColor(.white)
                                
                                Text("Bugünkü Bardak")
                                    .font(.subheadline)
                                    .foregroundColor(.gray)
                            }
                        }
                        
                        Button {
                            if glassesDrunk < dailyGoal {
                                withAnimation {
                                    glassesDrunk += 1
                                }
                                showMessage("Tebrikler! +1 bardak su içtin 💧")
                            } else {
                                showMessage("Günlük hedefini tamamladın! 🎉")
                            }
                        } label: {
                            HStack {
                                Image(systemName: "cup.and.saucer.fill")
                                Text("SU İÇTİM")
                                    .fontWeight(.bold)
                            }
                            .font(.headline)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 18)
                            .background(Color.blue)
                            .cornerRadius(14)
                        }
                        .padding(.horizontal, 30)
                        
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Image(systemName: "bell.fill")
                                    .foregroundColor(.green)
                                Text("Hatırlatıcı Ayarları")
                                    .font(.headline)
                                    .foregroundColor(.white)
                            }
                            
                            HStack {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Aktif")
                                        .font(.subheadline.weight(.medium))
                                        .foregroundColor(.white)
                                    Text("Su içmemi hatırlat")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                                
                                Toggle("", isOn: $isReminderActive)
                                    .labelsHidden()
                                    .tint(.green)
                                    .onChange(of: isReminderActive) { newValue in
                                        if newValue {
                                            requestNotificationPermission()
                                            scheduleWaterReminders()
                                        } else {
                                            cancelWaterReminders()
                                        }
                                    }
                            }
                            
                            if isReminderActive {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text("Her \(Int(reminderInterval)) saatte bir hatırlat")
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                    
                                    Slider(value: $reminderInterval, in: 1...6, step: 1)
                                        .tint(.green)
                                        .onChange(of: reminderInterval) { _ in
                                            if isReminderActive {
                                                scheduleWaterReminders()
                                            }
                                        }
                                }
                                .padding(.top, 6)
                            }
                        }
                        .padding()
                        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
                        .cornerRadius(14)
                        .padding(.horizontal)
                        
                        Text("Su içmek odaklanmayı artırır ve yorgunluğu azaltır.")
                            .font(.caption)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                            .padding(.bottom, 120)
                    }
                }
            }
        }
        .alert(alertMessage, isPresented: $showAlert) {
            Button("Tamam", role: .cancel) { }
        }
    }
    
    private func showMessage(_ msg: String) {
        alertMessage = msg
        showAlert = true
    }
    
    private func requestNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if !granted {
                DispatchQueue.main.async {
                    isReminderActive = false
                    showMessage("Bildirim izni verilmedi. Ayarlardan izin vermen gerekiyor.")
                }
            }
        }
    }
    
    private func scheduleWaterReminders() {
        cancelWaterReminders()
        
        let content = UNMutableNotificationContent()
        content.title = "Su İçme Zamanı"
        content.body = "Bir bardak su içmeyi unutma! 💧"
        content.sound = .default
        
        let interval = reminderInterval * 3600
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: true)
        
        let request = UNNotificationRequest(identifier: "waterReminder", content: content, trigger: trigger)
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Bildirim planlanamadı: \(error.localizedDescription)")
            }
        }
    }
    
    private func cancelWaterReminders() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["waterReminder"])
    }
}

// MARK: - Ortak Modeller & Yardımcılar
struct TaskItem: Identifiable, Codable {
    var id: UUID = UUID()
    var title: String
    var notes: String
    var duration: String
    var category: String
    var difficulty: String
    var isCompleted: Bool = false
}

struct HabitItem: Identifiable, Codable {
    var id: UUID = UUID()
    var title: String
}

struct RadioButton: View {
    let icon: String
    let title: String
    var isActive: Bool = false
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 26))
                    .foregroundColor(isActive ? .blue : .white)
                    .frame(width: 62, height: 62)
                    .background(isActive ? Color.blue.opacity(0.25) : Color(red: 0.18, green: 0.20, blue: 0.22))
                    .cornerRadius(16)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(isActive ? Color.blue : Color.clear, lineWidth: 2)
                    )
                
                Text(title)
                    .font(.caption.weight(.medium))
                    .foregroundColor(isActive ? .blue : .gray)
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct LeaderItem: View {
    let name: String
    let xp: String
    var isGradient: Bool = false
    
    var body: some View {
        VStack(spacing: 6) {
            ZStack {
                if isGradient {
                    Circle()
                        .fill(LinearGradient(colors: [.purple, .blue, .cyan], startPoint: .topLeading, endPoint: .bottomTrailing))
                        .frame(width: 56, height: 56)
                } else {
                    Circle()
                        .fill(Color.gray.opacity(0.4))
                        .frame(width: 56, height: 56)
                        .overlay(Image(systemName: "person.fill").foregroundColor(.white))
                }
                
                Circle()
                    .fill(Color.green)
                    .frame(width: 12, height: 12)
                    .offset(x: 20, y: 20)
            }
            
            Text(name)
                .font(.subheadline.weight(.medium))
                .foregroundColor(.white)
            
            Text(xp)
                .font(.caption2)
                .foregroundColor(.gray)
        }
    }
}

struct StatBox: View {
    let icon: String
    let value: String
    let label: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Image(systemName: icon)
                .foregroundColor(color)
                .font(.title2)
            Text(value)
                .font(.title3.bold())
                .foregroundColor(.white)
            Text(label)
                .font(.caption2)
                .foregroundColor(.gray)
        }
        .frame(maxWidth: .infinity)
    }
}

struct StatBadge: View {
    let title: String
    let value: String
    var isStreak: Bool = false
    
    var body: some View {
        VStack(spacing: 2) {
            if isStreak {
                HStack(spacing: 2) {
                    Image(systemName: "flame.fill")
                        .font(.caption2)
                    Text("5d")
                        .font(.caption2.weight(.bold))
                }
                .foregroundColor(.orange)
            } else {
                Text(title)
                    .font(.caption2)
                    .foregroundColor(.gray)
                Text(value)
                    .font(.caption.weight(.bold))
                    .foregroundColor(.white)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .background(Color(red: 0.18, green: 0.19, blue: 0.21))
        .cornerRadius(8)
    }
}

struct TaskRow: View {
    let task: TaskItem
    let onToggle: () -> Void
    
    var body: some View {
        HStack {
            Button(action: onToggle) {
                Image(systemName: task.isCompleted ? "checkmark.circle.fill" : "circle")
                    .foregroundColor(task.isCompleted ? .green : .gray)
                    .font(.title3)
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(task.title)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(task.isCompleted ? .gray : .white)
                    .strikethrough(task.isCompleted)
                
                HStack(spacing: 8) {
                    Text(task.category)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.green.opacity(0.2))
                        .foregroundColor(.green)
                        .cornerRadius(4)
                    
                    Text(task.difficulty)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color.purple.opacity(0.2))
                        .foregroundColor(.purple)
                        .cornerRadius(4)
                    
                    if !task.duration.isEmpty {
                        Text(task.duration)
                            .font(.caption2)
                            .foregroundColor(.gray)
                    }
                }
            }
            
            Spacer()
        }
        .padding()
        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
        .cornerRadius(10)
    }
}

struct AchievementCard: View {
    let title: String
    let desc: String
    let isUnlocked: Bool
    let icon: String
    
    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(isUnlocked ? .yellow : .gray)
                .frame(width: 44, height: 44)
                .background(isUnlocked ? Color.yellow.opacity(0.2) : Color.gray.opacity(0.2))
                .cornerRadius(12)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundColor(isUnlocked ? .white : .gray)
                Text(desc)
                    .font(.caption)
                    .foregroundColor(.gray)
            }
            
            Spacer()
            
            if isUnlocked {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            } else {
                Image(systemName: "lock.fill")
                    .foregroundColor(.gray)
            }
        }
        .padding()
        .background(Color(red: 0.12, green: 0.14, blue: 0.16))
        .cornerRadius(12)
    }
}

// ==================== SES OYNATICI (LOCAL DOSYALAR) ====================
class AudioManager: ObservableObject {
    static let shared = AudioManager()
    private var player: AVQueuePlayer?
    private var looper: AVPlayerLooper?
    @Published var currentSound: String? = nil
    
    private init() {
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)
    }
    
    func play(named name: String, fileName: String, fileExtension: String = "mp3") {
        if currentSound == name {
            stop()
            return
        }
        
        stop()
        
        guard let url = Bundle.main.url(forResource: fileName, withExtension: fileExtension) else {
            print("Ses dosyası bulunamadı: \(fileName).\(fileExtension)")
            return
        }
        
        let item = AVPlayerItem(url: url)
        let queuePlayer = AVQueuePlayer(playerItem: item)
        looper = AVPlayerLooper(player: queuePlayer, templateItem: item)
        
        player = queuePlayer
        queuePlayer.play()
        currentSound = name
    }
    
    func stop() {
        player?.pause()
        player = nil
        looper = nil
        currentSound = nil
    }
}

#Preview {
    ContentView()
}
