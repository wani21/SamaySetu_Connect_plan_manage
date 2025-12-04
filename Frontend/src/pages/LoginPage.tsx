import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiMail, FiLock, FiEye, FiEyeOff } from 'react-icons/fi';
import { motion } from 'framer-motion';
import toast from 'react-hot-toast';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';
import { authAPI, teacherAPI } from '../services/api';
import { useAuthStore } from '../store/authStore';
import logo from '../assets/logo.png';
import bannerVideo from '../assets/banner_video1.mp4';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<any>({});

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  const validate = () => {
    const newErrors: any = {};
    if (!formData.email) newErrors.email = 'Email is required';
    if (!formData.password) newErrors.password = 'Password is required';
    return newErrors;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation(); // Prevent event bubbling
    
    const newErrors = validate();
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // Validate college email
    if (!formData.email.endsWith('@mitaoe.ac.in')) {
      toast.error('Please use your college email (@mitaoe.ac.in)', { duration: 5000 });
      setErrors({ email: 'Only college email addresses are allowed' });
      return;
    }

    setIsLoading(true);
    try {
      const response = await authAPI.login(formData);
      const { email, token, role } = response.data;
      
      // Store token first so subsequent API calls work
      login({ email, token, role, name: '' });
      
      // Fetch full profile data to get real name
      try {
        const profileResponse = await teacherAPI.getProfile();
        const profileData = profileResponse.data;
        // Update with real name from profile
        login({ email, token, role, name: profileData.name || email.split('@')[0] });
      } catch (profileError) {
        // If profile fetch fails, use email as fallback
        const name = email.split('@')[0].replace(/\./g, ' ').replace(/\d+/g, '').trim();
        login({ email, token, role, name });
      }
      
      toast.success('Login successful!');
      
      // Navigate based on role
      if (role === 'ADMIN') {
        navigate('/admin/dashboard');
      } else {
        navigate('/dashboard');
      }
    } catch (error: any) {
      console.error('Login error:', error);
      let message = 'Login failed. Please check your credentials.';
      let fieldErrors: any = {};
      
      // Handle specific error cases
      if (error.response) {
        const status = error.response.status;
        const data = error.response.data;
        
        if (status === 401) {
          message = 'Incorrect password. Please try again.';
          fieldErrors.password = 'Incorrect password';
        } else if (status === 404) {
          message = 'Account not found. Please check your email or register.';
          fieldErrors.email = 'Account not found';
        } else if (status === 403) {
          if (typeof data === 'string' && data.toLowerCase().includes('verify')) {
            message = 'Email not verified. Please check your inbox and verify your email.';
          } else {
            message = 'Account not verified. Please verify your email first.';
          }
          fieldErrors.email = 'Email not verified';
        } else if (typeof data === 'string') {
          message = data;
        } else if (data.message) {
          message = data.message;
        }
      } else if (error.message) {
        message = error.message;
      }
      
      toast.error(message, { duration: 6000 });
      setErrors(fieldErrors);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Full Background Video */}
      <video
        autoPlay
        loop
        muted
        playsInline
        className="absolute inset-0 w-full h-full object-cover"
      >
        <source src={bannerVideo} type="video/mp4" />
        Your browser does not support the video tag.
      </video>

      {/* Content Overlay */}
      <div className="relative z-10 min-h-screen flex flex-col lg:flex-row">
        
        {/* Left Side - Branding */}
        <div className="flex-1 flex flex-col p-8 lg:p-12">
          {/* Logo at Top Left with Background */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6 }}
            className="inline-block mb-8"
          >
            <div className="bg-white/60 backdrop-blur-sm rounded-2xl p-3 shadow-lg inline-block">
              <img src={logo} alt="MIT AOE" className="h-20 lg:h-24 block" />
            </div>
          </motion.div>

          {/* Branding Text Higher Up */}
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="hidden lg:block text-white mt-12"
          >
            <h1 className="text-5xl font-bold mb-4 drop-shadow-lg">SamaySetu</h1>
            <p className="text-2xl mb-2 drop-shadow-md">Timetable Management System</p>
            <p className="text-xl opacity-90 drop-shadow-md">MIT Academy of Engineering</p>
          </motion.div>
        </div>

        {/* Right Side - Login Form with Transparent Box */}
        <div className="w-full lg:w-[520px] flex items-center justify-center p-6 lg:p-12">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="w-full max-w-lg"
          >
            {/* Mobile Logo */}
            <div className="lg:hidden text-center mb-8">
              <img src={logo} alt="MIT AOE" className="h-20 mx-auto mb-4 drop-shadow-lg" />
              <h1 className="text-3xl font-bold text-white mb-2 drop-shadow-lg">SamaySetu</h1>
              <p className="text-white drop-shadow-md">Timetable Management System</p>
            </div>

            {/* Semi-transparent Login Box */}
            <div className="bg-white/80 backdrop-blur-md rounded-2xl shadow-2xl p-10 border border-white/20">
              <div className="text-center mb-8">
                <h2 className="text-3xl font-bold text-gray-900">Welcome Back!</h2>
                <p className="text-sm text-gray-600 mt-2">Sign in to your account to continue</p>
              </div>

            {/* Sign In Form */}
            <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            <Input
              label="Email Address"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              error={errors.email}
              icon={<FiMail />}
              placeholder="your.email@mitaoe.ac.in"
              autoComplete="email"
            />

            <div className="relative">
              <Input
                label="Password"
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={formData.password}
                onChange={handleChange}
                error={errors.password}
                icon={<FiLock />}
                placeholder="Enter your password"
                autoComplete="current-password"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-[38px] text-gray-400 hover:text-gray-600"
              >
                {showPassword ? <FiEyeOff size={20} /> : <FiEye size={20} />}
              </button>
            </div>

            <div className="flex items-center justify-between text-sm">
              <label className="flex items-center">
                <input type="checkbox" className="mr-2 rounded" />
                <span className="text-gray-600">Remember me</span>
              </label>
              <Link
                to="/forgot-password"
                className="text-primary-800 hover:text-primary-900 font-medium"
              >
                Forgot Password?
              </Link>
            </div>

            <Button
              type="submit"
              variant="primary"
              className="w-full"
              isLoading={isLoading}
            >
              Sign In
            </Button>
          </form>

          {/* Register Link */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              Don't have an account?{' '}
              <Link
                to="/register"
                className="text-primary-800 hover:text-primary-900 font-medium"
              >
                Register Now
              </Link>
            </p>
          </div>

          {/* Footer */}
          <div className="mt-6 pt-6 border-t border-gray-200 text-center text-xs text-gray-500">
            <p>© 2025 MIT Academy of Engineering</p>
          </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
};
