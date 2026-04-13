import React, { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { FiLock, FiEye, FiEyeOff, FiArrowLeft } from 'react-icons/fi';
import { motion } from 'framer-motion';
import toast from 'react-hot-toast';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';
import { authAPI } from '../services/api';
import logo from '../assets/logo.png';
import bannerVideo from '../assets/banner_video1.mp4';

export const ResetPasswordPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const error = searchParams.get('error');
  
  const [formData, setFormData] = useState({
    newPassword: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<any>({});

  // Show error if token is invalid or expired
  React.useEffect(() => {
    if (error) {
      toast.error(error);
    }
    if (!token && !error) {
      toast.error('Invalid reset link');
      navigate('/login');
    }
  }, [error, token, navigate]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  const validate = () => {
    const newErrors: any = {};
    
    if (!formData.newPassword) {
      newErrors.newPassword = 'Password is required';
    } else if (formData.newPassword.length < 6) {
      newErrors.newPassword = 'Password must be at least 6 characters';
    }
    
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (formData.newPassword !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    
    return newErrors;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    if (!token) {
      toast.error('Invalid reset link');
      return;
    }

    const newErrors = validate();
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      Object.values(newErrors).forEach((error: any) => {
        toast.error(error);
      });
      return;
    }

    setIsLoading(true);
    try {
      await authAPI.resetPassword({
        token,
        newPassword: formData.newPassword,
      });
      
      toast.success('Password reset successful! You can now login.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (error: any) {
      console.error('Reset password error:', error);
      let message = 'Password reset failed. Please try again or request a new reset link.';
      
      if (error.response) {
        const data = error.response.data;
        if (typeof data === 'string') {
          // Clean up any technical error prefixes
          message = data.replace(/^An unexpected error occurred:\s*\d+\s*[A-Z_]+\s*"?/, '').replace(/["']$/, '');
        } else if (data.message) {
          message = data.message;
        } else if (data.error) {
          message = data.error;
        }
      } else if (error.message) {
        message = error.message;
      }
      
      toast.error(message);
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

        {/* Right Side - Reset Password Form with Transparent Box */}
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

            {/* Semi-transparent Reset Password Box */}
            <div className="bg-white/80 backdrop-blur-md rounded-2xl shadow-2xl p-10 border border-white/20">
              {/* Back to Login Link */}
              <Link
                to="/login"
                className="inline-flex items-center gap-2 text-primary-800 hover:text-primary-900 font-medium mb-6 transition-colors"
              >
                <FiArrowLeft />
                <span>Back to Login</span>
              </Link>

              <div className="text-center mb-8">
                <h2 className="text-3xl font-bold text-gray-900">Reset Password</h2>
                <p className="text-sm text-gray-600 mt-2">
                  Enter your new password below
                </p>
              </div>

              {/* Reset Password Form */}
              <form onSubmit={handleSubmit} className="space-y-5" noValidate>
                <div className="relative">
                  <Input
                    label="New Password"
                    type={showPassword ? 'text' : 'password'}
                    name="newPassword"
                    value={formData.newPassword}
                    onChange={handleChange}
                    error={errors.newPassword}
                    icon={<FiLock />}
                    placeholder="Min. 6 characters"
                    autoComplete="new-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-[38px] text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    {showPassword ? <FiEyeOff size={20} /> : <FiEye size={20} />}
                  </button>
                </div>

                <div className="relative">
                  <Input
                    label="Confirm New Password"
                    type={showConfirmPassword ? 'text' : 'password'}
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    error={errors.confirmPassword}
                    icon={<FiLock />}
                    placeholder="Confirm your password"
                    autoComplete="new-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-[38px] text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    {showConfirmPassword ? <FiEyeOff size={20} /> : <FiEye size={20} />}
                  </button>
                </div>

                <Button
                  type="submit"
                  variant="primary"
                  className="w-full"
                  isLoading={isLoading}
                >
                  Reset Password
                </Button>
              </form>

              {/* Footer */}
              <div className="mt-6 pt-6 border-t border-gray-200 text-center text-xs text-gray-500">
                <p>© 2026 MIT Academy of Engineering</p>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
};
