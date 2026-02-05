# 🏠 PlantaHUB

> **Digital Marketplace for Architectural Plans** - Transform your dream home into reality with professional, ready-to-build designs.

[![React](https://img.shields.io/badge/React-19.2.0-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9.3-blue.svg)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-4.1.18-38B2AC.svg)](https://tailwindcss.com/)
[![Vite](https://img.shields.io/badge/Vite-7.2.4-646CFF.svg)](https://vitejs.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-6DB33F.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00.svg)](https://openjdk.java.net/)

## 🚀 What is PlantaHUB?

PlantaHUB is a modern e-commerce platform specializing in **architectural house and chalet plans**. We provide complete, professional building documentation that customers can purchase and download instantly.

### 🏡 Product Catalog

**Houses (Casas)**
- 🏠 **Confort** - 80m² Compact Modern Home
- 🏘️ **Prime** - 150m² Family House
- 🏛️ **Diamond** - 300m² Premium Residence

**Chalets (Chalés)**
- 🏕️ **Confort** - 55m² Cozy Retreat
- 🏞️ **Prime** - 85m² Family Chalet with Deck
- ✨ **Diamond** - 120m² Premium Mountain Lodge

### 📋 What's Included

✅ **Complete Documentation Package**
- Architectural Plans
- Hydraulic Systems
- Electrical Layouts
- Structural Engineering
- Landscaping Designs
- Material Lists
- Construction Guides

✅ **Multiple Formats**: BIM, DWG, PDF
✅ **Instant Download**
✅ **Fully Customizable**

## 🛠️ Tech Stack

**Frontend**
- **Framework**: React 19 + TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS 4.x
- **Routing**: React Router DOM
- **Icons**: Lucide React

**Backend**
- **Framework**: Spring Boot 3.4.2
- **Language**: Java 21
- **Security**: JWT Authentication
- **Database**: PostgreSQL with Flyway migrations
- **Documentation**: Swagger/OpenAPI

**Architecture**: Monorepo Structure

## 🚀 Quick Start

### Frontend
```bash
# Clone the repository
git clone https://github.com/yourusername/PlantaHUB.git

# Navigate to web app
cd PlantaHUB/apps/web/plantahub-web

# Install dependencies
npm install

# Start development server
npm run dev
```

### Backend API
```bash
# Navigate to API
cd PlantaHUB/apps/api/plantahub-api

# Run with Maven
./mvnw spring-boot:run

# API will be available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

## 📁 Project Structure

```
PlantaHUB/
├── apps/
│   ├── api/
│   │   └── plantahub-api/   # Spring Boot API
│   │       ├── src/main/java/
│   │       │   ├── domain/     # Domain entities
│   │       │   ├── repository/ # Data access layer
│   │       │   ├── security/   # JWT & Security config
│   │       │   ├── service/    # Business logic
│   │       │   └── web/        # Controllers & DTOs
│   │       └── src/main/resources/
│   │           └── db/migration/ # Flyway migrations
│   └── web/
│       └── plantahub-web/   # React frontend
│           ├── src/
│           │   ├── components/  # Reusable UI components
│           │   ├── pages/      # Route pages
│           │   ├── data/       # Product data & configs
│           │   ├── types/      # TypeScript definitions
│           │   └── styles/     # Global styles
│           └── public/         # Static assets
├── docs/
│   └── sql/                 # Database documentation
└── README.md
```

## 🎯 Features

**Frontend**
- 📱 **Responsive Design** - Works on all devices
- ⚡ **Fast Performance** - Optimized with Vite
- 🎨 **Modern UI** - Clean, professional interface
- 🔍 **Product Showcase** - Detailed product pages
- 📊 **Comprehensive Info** - Specs, FAQs, testimonials
- 🛒 **E-commerce Ready** - Built for digital sales

**Backend API**
- 🔐 **JWT Authentication** - Secure user authentication
- 👤 **User Management** - Registration and login
- 📚 **API Documentation** - Interactive Swagger UI
- 🗄️ **Database Integration** - PostgreSQL with migrations
- 🛡️ **Security** - Spring Security configuration

## 🚧 Development

```bash
# Development
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint

# Format code
npm run format
```

## 🎨 Design System

- **Colors**: Eva Design System
- **Typography**: Modern, readable fonts
- **Components**: Modular, reusable UI elements
- **Layout**: Mobile-first responsive design

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🌟 Support

If you like this project, please give it a ⭐ on GitHub!

---

**Built with ❤️ for the architecture and construction community**