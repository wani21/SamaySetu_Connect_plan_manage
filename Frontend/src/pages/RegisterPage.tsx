import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiMail, FiLock, FiUser, FiPhone, FiEye, FiEyeOff, FiBriefcase } from 'react-icons/fi';
import { motion } from 'framer-motion';
import toast from 'react-hot-toast';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';
import { authAPI } from '../services/api';
import logo from '../assets/logo.png';
import bannerVideo from '../assets/banner_video1.mp4';

export const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  
  const [formData, setFormData] = useState({
    name: '',
    employeeId: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
    specialization: '',
  });
  
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<any>({});

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: '' });
  };

  const validate = () => {
    const newErrors: any = {};
    
    if (!formData.name) newErrors.name = 'Name is required';
    if (!formData.employeeId) newErrors.employeeId = 'Employee ID is required';
    if (!formData.email) {
      newErrors.email = 'Email is required';
    } else if (!formData.email.endsWith('@mitaoe.ac.in')) {
      newErrors.email = 'Only college email (@mitaoe.ac.in) is allowed';
    }
    if (!formData.phone) newErrors.phone = 'Phone number is required';
    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }
    
    return newErrors;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const newErrors = validate();
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      const { confirmPassword, ...registerData } = formData;
      await authAPI.register(registerData);
      
      toast.success('Registration successful! Please check your email to verify your account.');
      setTimeout(() => navigate('/login'), 2000);
    } catch (error: any) {
      const message = error.response?.data || 'Registration failed. Please try again.';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="h-screen relative overflow-hidden">
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
      <div className="relative z-10 h-screen flex flex-col lg:flex-row overflow-hidden">
        
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

        {/* Right Side - Registration Form with Transparent Box */}
        <div className="w-full lg:w-[700px] flex items-center justify-center p-4 lg:p-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="w-full max-w-3xl my-4"
          >
            {/* Mobile Logo */}
            <div className="lg:hidden text-center mb-6">
              <img src={logo} alt="MIT AOE" className="h-16 mx-auto mb-4 drop-shadow-lg" />
              <h1 className="text-2xl font-bold text-white mb-2 drop-shadow-lg">SamaySetu</h1>
              <p className="text-white drop-shadow-md">Timetable Management System</p>
            </div>

            {/* Semi-transparent Registration Box */}
            <div className="bg-white/80 backdrop-blur-md rounded-2xl shadow-2xl p-8 border border-white/20 max-h-[90vh] overflow-y-auto">
              <div className="text-center mb-6">
                <h2 className="text-2xl font-bold text-gray-900">Create Account</h2>
                <p className="text-sm text-gray-600 mt-1">Register for SamaySetu</p>
              </div>

            {/* Registration Form */}
            <form onSubmit={handleSubmit} className="space-y-3" noValidate>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <Input
                label="Full Name"
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                error={errors.name}
                icon={<FiUser />}
                placeholder="Name"
              />

              <Input
                label="Employee ID"
                type="text"
                name="employeeId"
                value={formData.employeeId}
                onChange={handleChange}
                error={errors.employeeId}
                icon={<FiBriefcase />}
                placeholder="EMP001"
              />
            </div>

            <Input
              label="College Email"
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              error={errors.email}
              icon={<FiMail />}
              placeholder="your.name@mitaoe.ac.in"
              autoComplete="email"
            />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <Input
                label="Phone Number"
                type="tel"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                error={errors.phone}
                icon={<FiPhone />}
                placeholder="1234567890"
              />

              <Input
                label="Specialization"
                type="text"
                name="specialization"
                value={formData.specialization}
                onChange={handleChange}
                icon={<FiBriefcase />}
                placeholder="Computer Science"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="relative">
                <Input
                  label="Password"
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  error={errors.password}
                  icon={<FiLock />}
                  placeholder="Min. 6 characters"
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-[38px] text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <FiEyeOff size={20} /> : <FiEye size={20} />}
                </button>
              </div>

              <div className="relative">
                <Input
                  label="Confirm Password"
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  error={errors.confirmPassword}
                  icon={<FiLock />}
                  placeholder="Confirm password"
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  className="absolute right-3 top-[38px] text-gray-400 hover:text-gray-600"
                >
                  {showConfirmPassword ? <FiEyeOff size={20} /> : <FiEye size={20} />}
                </button>
              </div>
            </div>

            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-800">
              <p className="font-medium mb-1">📧 Email Verification Required</p>
              <p>After registration, please check your college email for a verification link.</p>
            </div>

            <Button
              type="submit"
              variant="primary"
              className="w-full"
              isLoading={isLoading}
            >
              Register
            </Button>
          </form>

          {/* Login Link */}
          <div className="mt-6 text-center">
            <p className="text-gray-600">
              Already have an account?{' '}
              <Link
                to="/login"
                className="text-primary-800 hover:text-primary-900 font-medium"
              >
                Sign In
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
