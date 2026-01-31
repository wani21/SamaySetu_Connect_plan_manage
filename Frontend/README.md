# 🎨 SamaySetu Frontend

Modern, responsive React frontend for SamaySetu Timetable Management System with MIT Academy of Engineering theme.

## ✨ Latest Features (v3.0)

- ✅ **Academic Year Separation** - Complete isolation of academic years with department copying
- ✅ **Enhanced Staff Management** - Bulk Registration, approval workflow, manual creation
- ✅ **Advanced Academic Structure** - Hierarchical navigation with batches and divisions
- ✅ **Time Slot Types** - Multiple schedule support (Schedule 1 & Schedule 2)
- ✅ **Class Management** - Class teacher and CR assignment for divisions
- ✅ **CSV Operations** - Bulk Registration/download for staff and courses
- ✅ **MIT AOE Color Scheme** - Navy Blue & Cyan institutional branding
- ✅ **Complete Authentication Flow** - Login, Register, Email Verification, Password Reset
- ✅ **Role-Based Dashboards** - Teacher and Admin interfaces
- ✅ **Responsive Design** - Mobile, Tablet, Desktop optimized
- ✅ **Modern UI/UX** - Smooth animations and intuitive navigation

## 🚀 Quick Start

### 1. Install Dependencies
```bash
cd Frontend
npm install
```

### 2. Start Development Server
```bash
npm run dev
```

Visit: `http://localhost:5173`

### 3. Build for Production
```bash
npm run build
```

## 📁 Project Structure

```
Frontend/
├── src/
│   ├── assets/          # Images, logo, icons
│   ├── components/
│   │   ├── common/      # Reusable UI components (Button, Input, Card, Modal)
│   │   ├── auth/        # Authentication components
│   │   ├── dashboard/   # Dashboard components
│   │   ├── admin/       # Admin management pages
│   │   └── layout/      # Layout components (Navbar, Sidebar)
│   ├── pages/           # Main page components
│   ├── services/        # API integration services
│   ├── store/           # State management (Zustand)
│   ├── utils/           # Helper functions and utilities
│   ├── types/           # TypeScript type definitions
│   ├── App.tsx          # Main application component
│   ├── main.tsx         # Application entry point
│   └── index.css        # Global styles and Tailwind imports
├── public/              # Static assets
├── package.json         # Dependencies and scripts
├── vite.config.ts       # Vite configuration
└── tailwind.config.js   # Tailwind CSS configuration
```

## 🎨 MIT Academy of Engineering Theme

### Color Palette:
- **Primary Navy**: `#1a237e` - Headers, primary buttons, navigation
- **Primary Blue**: `#283593` - Main actions, active states
- **Secondary Cyan**: `#00bcd4` - Accents, highlights, links
- **Success Green**: `#4caf50` - Success messages, confirmations
- **Warning Orange**: `#ff9800` - Warnings, pending states
- **Error Red**: `#f44336` - Errors, deletions, critical actions
- **Gray Scale**: Various shades for text, borders, backgrounds

### Typography:
- **Font Family**: Inter (modern, readable)
- **Headings**: Bold weights for hierarchy
- **Body Text**: Regular weight for readability

## 🔐 Authentication System

### Complete Flow:
1. **Registration** → Email Verification → Login
2. **Login** → Role-based Dashboard (Teacher/Admin)
3. **Forgot Password** → Reset Email → New Password → Login
4. **First-time Login** → Password Change (for admin-created accounts)

### Security Features:
- JWT token-based authentication
- Automatic token refresh
- Protected routes with role validation
- Session management
- Password strength requirements

## 📱 Pages & Features

### 🌐 Public Pages:
- **Login Page** - Secure authentication with validation
- **Registration Page** - Currently disabled (admin-only staff creation)
- **Email Verification** - Token-based email confirmation
- **Forgot Password** - Password reset request
- **Reset Password** - New password setup

### 👨‍🏫 Teacher Dashboard:
- Personal profile management
- Schedule viewing
- Course assignments
- Profile updates (restricted fields)
- Password change functionality

### 👨‍💼 Admin Dashboard:
- **Academic Structure Management**:
  - Academic Years (create, manage, set current)
  - Departments (academic year specific, copy between years)
  - Years Overview (FY, SY, TY, BTech organization)
  - Divisions (with class teacher and CR assignment)
  - Courses (semester-wise, Bulk Registration via CSV)
  - Batches (student group management)

- **Staff Management**:
  - Bulk staff upload via CSV
  - Manual staff creation
  - Staff approval workflow
  - Profile updates and management
  - Template downloads

- **Resource Management**:
  - Time Slots (multiple types for flexible scheduling)
  - Rooms/Classrooms (with equipment tracking)
  - Academic calendar management

- **System Administration**:
  - User role management
  - System configuration
  - Data import/export operations

## 🛠️ Technology Stack

### Core Technologies:
- **React 18** - Modern UI library with hooks
- **TypeScript** - Type safety and better development experience
- **Vite** - Fast build tool and development server
- **Tailwind CSS** - Utility-first CSS framework

### State & Data Management:
- **Zustand** - Lightweight state management
- **Axios** - HTTP client with interceptors
- **React Query** - Server state management (planned)

### UI & UX:
- **Framer Motion** - Smooth animations and transitions
- **React Hot Toast** - Beautiful toast notifications
- **React Icons** - Comprehensive icon library
- **Headless UI** - Accessible UI components

### Routing & Navigation:
- **React Router v6** - Client-side routing
- **Protected Routes** - Role-based access control
- **Dynamic Navigation** - Context-aware menu items

## 🔧 Configuration

### API Configuration
Update in `src/services/api.ts`:
```typescript
const API_BASE_URL = 'http://localhost:8083';

// JWT token management
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### Environment Variables
Create `.env` file in Frontend directory:
```env
VITE_API_URL=http://localhost:8083
VITE_APP_NAME=SamaySetu
VITE_COLLEGE_NAME=MIT Academy of Engineering
```

### Vite Configuration
Proxy setup in `vite.config.ts`:
```typescript
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8083',
      '/admin': 'http://localhost:8083',
      '/auth': 'http://localhost:8083'
    }
  }
});
```

## 🎯 Default Access

### Admin Access:
- **Email**: `admin@mitaoe.ac.in`
- **Password**: `admin123`
- **Role**: ADMIN
- **Access**: Full system administration

### Staff Access:
- **Creation**: Admin creates staff accounts
- **Default Password**: `mitaoe@123`
- **First Login**: Must change password
- **Email Domain**: Must be `@mitaoe.ac.in`

## 🔄 Academic Year Workflow

### 1. Academic Year Setup:
- Create new academic year (e.g., 2024-25)
- Set as current if active
- Define start and end dates

### 2. Department Management:
- Create departments for the academic year
- Or copy from previous year using "Copy from Other Year"
- Assign HODs and available years (FY, SY, TY, BTech)

### 3. Structure Building:
- Navigate through: Academic Years → Year Overview → Department Detail
- Create divisions with class teachers and CRs
- Add courses semester-wise or via CSV upload
- Organize students into batches

### 4. Schedule Configuration:
- Set up time slots with types (Schedule 1/2)
- Assign time slot types to divisions
- Configure rooms and resources

## 🐛 Troubleshooting

### Common Issues:

**CORS Errors:**
```bash
# Ensure backend is running on port 8083
# Check proxy configuration in vite.config.ts
```

**Module Not Found:**
```bash
cd Frontend
npm install
```

**Port Already in Use:**
```bash
# Change port in vite.config.ts
server: { port: 3000 }
```

**Authentication Issues:**
```bash
# Clear browser storage
localStorage.clear();
# Or check JWT token expiration
```

**API Connection:**
```bash
# Verify backend is running
curl http://localhost:8083/api/health
```

## 📚 Development Guidelines

### Component Structure:
```typescript
// Use TypeScript interfaces
interface ComponentProps {
  title: string;
  onAction: () => void;
}

// Functional components with hooks
const Component: React.FC<ComponentProps> = ({ title, onAction }) => {
  // Component logic
};
```

### State Management:
```typescript
// Use Zustand for global state
const useAuthStore = create<AuthState>((set) => ({
  user: null,
  login: (user) => set({ user }),
  logout: () => set({ user: null })
}));
```

### API Integration:
```typescript
// Use consistent error handling
try {
  const response = await api.get('/endpoint');
  return response.data;
} catch (error) {
  toast.error(getErrorMessage(error));
  throw error;
}
```

## 🚀 Deployment

### Build for Production:
```bash
npm run build
```

### Preview Production Build:
```bash
npm run preview
```

### Deploy to Server:
```bash
# Build files will be in 'dist' directory
# Deploy to your web server (Apache, Nginx, etc.)
```

## 📈 Performance Optimizations

- **Code Splitting** - Lazy loading of routes
- **Bundle Optimization** - Tree shaking and minification
- **Image Optimization** - Compressed assets
- **Caching Strategy** - API response caching
- **Responsive Images** - Multiple sizes for different devices

## 🎉 Ready Features

Your complete frontend includes:
- ✅ **Academic Year Management** - Complete separation and copying
- ✅ **Staff Management System** - Bulk operations and approval workflow
- ✅ **Hierarchical Navigation** - Intuitive academic structure browsing
- ✅ **Modern UI Components** - Consistent design system
- ✅ **Responsive Design** - Works on all devices
- ✅ **Role-Based Access** - Secure authentication and authorization
- ✅ **CSV Operations** - Bulk data import/export
- ✅ **Real-time Validation** - Form validation and error handling
- ✅ **Toast Notifications** - User feedback system
- ✅ **Loading States** - Better user experience

## 📞 Support & Documentation

- **API Documentation**: `../SamaySetu_Postman_Collection.json`
- **Backend Setup**: `../Backend/README.md`
- **Database Schema**: `../add_batches_and_timeslot_types.sql`
- **Project Overview**: `../README.md`

## 🔮 Future Enhancements

- [ ] **Timetable Generation** - Automated scheduling
- [ ] **Conflict Detection** - Schedule overlap prevention
- [ ] **Mobile App** - React Native version
- [ ] **Offline Support** - PWA capabilities
- [ ] **Advanced Analytics** - Usage statistics and reports
- [ ] **Multi-language** - Internationalization support
- [ ] **Dark Mode** - Theme switching
- [ ] **Real-time Updates** - WebSocket integration

---

**Built with ❤️ for MIT Academy of Engineering, Alandi, Pune**

© 2026 MIT Academy of Engineering - SamaySetu Development Team
