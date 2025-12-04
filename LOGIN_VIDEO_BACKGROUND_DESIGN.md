# Login & Register Pages - Video Background Design

## Design Overview

Beautiful, modern design with full video background, branding on the left, and semi-transparent form box on the right.

## Key Features

### ✅ Full Video Background
- Video covers entire screen
- No cropping or cutting
- Full visibility maintained
- Professional appearance

### ✅ Logo Placement
- **Top-left corner** of the screen
- Visible on all screen sizes
- Drop shadow for visibility
- Size: h-20 (lg:h-24)

### ✅ Branding Text (Left Side, Middle)
- **SamaySetu** - Large, bold (text-5xl)
- **Timetable Management System** - Subtitle (text-2xl)
- **MIT Academy of Engineering** - Institution name (text-xl)
- White text with drop shadows
- Hidden on mobile, shown on desktop

### ✅ Semi-Transparent Form Box (Right Side)
- **Background**: `bg-white/95` (95% opacity)
- **Backdrop blur**: `backdrop-blur-md` for glass effect
- **Border**: `border-white/20` for subtle edge
- **Shadow**: `shadow-2xl` for depth
- **Rounded corners**: `rounded-2xl`
- **Width**: 450px (login), 550px (register)

### ✅ No Heavy Overlay
- Video remains fully visible
- Only subtle transparency on form box
- No dark overlays blocking video
- Clean, professional look

## Layout Structure

```
┌─────────────────────────────────────────────────┐
│  [Logo]                          [Form Box]     │
│                                  Semi-trans     │
│                                  95% white      │
│  [Branding Text]                 Backdrop blur  │
│  - SamaySetu                     Login/Register │
│  - Subtitle                      Fields         │
│  - Institution                                  │
│                                                 │
│  [Full Video Background - No overlay]          │
└─────────────────────────────────────────────────┘
```

## Technical Implementation

### Full Video Background
```tsx
<video
  autoPlay
  loop
  muted
  playsInline
  className="absolute inset-0 w-full h-full object-cover"
>
  <source src={bannerVideo} type="video/mp4" />
</video>
```

### Logo (Top-Left)
```tsx
<img 
  src={logo} 
  alt="MIT AOE" 
  className="h-20 lg:h-24 drop-shadow-lg" 
/>
```

### Branding Text (Middle-Left)
```tsx
<div className="hidden lg:block text-white">
  <h1 className="text-5xl font-bold mb-4 drop-shadow-lg">
    SamaySetu
  </h1>
  <p className="text-2xl mb-2 drop-shadow-md">
    Timetable Management System
  </p>
  <p className="text-xl opacity-90 drop-shadow-md">
    MIT Academy of Engineering
  </p>
</div>
```

### Semi-Transparent Form Box
```tsx
<div className="bg-white/95 backdrop-blur-md rounded-2xl shadow-2xl p-8 border border-white/20">
  {/* Form content */}
</div>
```

## Color & Transparency

- **Video**: Full visibility, no overlay
- **Form box**: 95% white opacity (`bg-white/95`)
- **Backdrop blur**: Medium blur for glass effect
- **Text on video**: White with drop shadows
- **Form text**: Dark gray for readability
- **Border**: 20% white opacity for subtle edge

## Responsive Design

### Desktop (lg and above)
- Logo at top-left
- Branding text visible on left side
- Form box on right (450px/550px width)
- Full video background

### Mobile/Tablet
- Logo centered at top
- Branding text below logo
- Form box full-width
- Full video background
- Scrollable content

## Animations

### Logo
- Slides in from left
- Fade in effect
- Duration: 0.6s

### Branding Text
- Slides in from left
- Fade in effect
- Duration: 0.6s
- Delay: 0.2s

### Form Box
- Slides up from bottom
- Fade in effect
- Duration: 0.6s
- Delay: 0.3s

## Typography

### Branding (White text on video)
- **SamaySetu**: text-5xl, font-bold
- **Subtitle**: text-2xl
- **Institution**: text-xl, opacity-90

### Form (Dark text on white)
- **Title**: text-2xl, font-bold
- **Subtitle**: text-sm
- **Labels**: text-sm
- **Footer**: text-xs

## Spacing

- Container padding: `p-8 lg:p-12`
- Form box padding: `p-8`
- Section margins: `mb-4` to `mb-6`
- Form fields: `space-y-4`

## Visual Effects

### Drop Shadows
- Logo: `drop-shadow-lg`
- Branding text: `drop-shadow-lg` and `drop-shadow-md`
- Form box: `shadow-2xl`

### Backdrop Blur
- Form box: `backdrop-blur-md`
- Creates frosted glass effect
- Maintains video visibility

### Borders
- Form box: `border border-white/20`
- Subtle edge definition
- Enhances glass effect

## Features

### Login Page
- Full video background
- Logo top-left
- Branding middle-left
- Semi-transparent form box (450px)
- Email and password fields
- Remember me checkbox
- Forgot password link
- Register link
- Footer

### Register Page
- Full video background
- Logo top-left
- Branding middle-left
- Semi-transparent form box (550px)
- Two-column form layout
- All registration fields
- Email verification notice
- Login link
- Footer

## Files Modified

1. **Frontend/src/pages/LoginPage.tsx**
   - Full video background
   - Logo at top-left
   - Branding text on left side
   - Semi-transparent form box (450px)
   - No heavy overlay

2. **Frontend/src/pages/RegisterPage.tsx**
   - Full video background
   - Logo at top-left
   - Branding text on left side
   - Semi-transparent form box (550px)
   - Two-column form layout

## Result

✅ Full video background (no cropping)
✅ Logo at top-left corner
✅ Branding text on left side (middle)
✅ Semi-transparent form box on right
✅ No heavy overlay blocking video
✅ Professional, modern design
✅ Smooth animations
✅ Responsive layout
✅ Glass morphism effect
✅ Excellent readability
