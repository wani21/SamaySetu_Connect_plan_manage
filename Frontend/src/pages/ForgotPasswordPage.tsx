import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { FiMail, FiArrowLeft } from 'react-icons/fi';
import { motion } from 'framer-motion';
import toast from 'react-hot-toast';
import { Input } from '../components/common/Input';
import { Button } from '../components/common/Button';
import { authAPI } from '../services/api';
import logo from '../assets/logo.png';
import bannerVideo from '../assets/banner_video1.mp4';

export const ForgotPasswordPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isEmailSent, setIsEmailSent] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
    setError('');
  };

  const validate = () => {
    if (!email) {
      return 'Email is required';
    }
    if (!email.endsWith('@mitaoe.ac.in')) {
      return 'Please use your college email (@mitaoe.ac.in)';
    }
    return '';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      toast.error(validationError);
      return;
    }

    setIsLoading(true);
    try {
      await authAPI.forgotPassword(email);
      setIsEmailSent(true);
      toast.success('Password reset link sent to your email!');
    } catch (error: any) {
      console.error('Forgot password error:', error);
      let message = 'Failed to send reset link. Please try again.';
      
      if (error.response) {
        const data = error.response.data;
        if (typeof data === 'string') {
          message = data;
        } else if (data.message) {
          message = data.message;
        }
      }
      
      toast.error(message);
      setError(message);
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

        {/* Right Side - Forgot Password Form with Transparent Box */}
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

            {/* Semi-transparent Forgot Password Box */}
            <div className="bg-white/80 backdrop-blur-md rounded-2xl shadow-2xl p-10 border border-white/20">
              {/* Back to Login Link */}
              <Link
                to="/login"
                className="inline-flex items-center gap-2 text-primary-800 hover:text-primary-900 font-medium mb-6 transition-colors"
              >
                <FiArrowLeft />
                <span>Back to Login</span>
              </Link>

              {!isEmailSent ? (
                <>
                  <div className="text-center mb-8">
                    <h2 className="text-3xl font-bold text-gray-900">Forgot Password?</h2>
                    <p className="text-sm text-gray-600 mt-2">
                      No worries! Enter your email and we'll send you reset instructions.
                    </p>
                  </div>

                  {/* Forgot Password Form */}
                  <form onSubmit={handleSubmit} className="space-y-5" noValidate>
                    <Input
                      label="Email Address"
                      type="email"
                      name="email"
                      value={email}
                      onChange={handleChange}
                      error={error}
                      icon={<FiMail />}
                      placeholder="your.email@mitaoe.ac.in"
                      autoComplete="email"
                    />

                    <Button
                      type="submit"
                      variant="primary"
                      className="w-full"
                      isLoading={isLoading}
                    >
                      Send Reset Link
                    </Button>
                  </form>
                </>
              ) : (
                <>
                  <div className="text-center mb-8">
                    <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                      <FiMail className="text-green-600 text-3xl" />
                    </div>
                    <h2 className="text-3xl font-bold text-gray-900 mb-2">Check Your Email</h2>
                    <p className="text-sm text-gray-600">
                      We've sent password reset instructions to
                    </p>
                    <p className="text-sm font-medium text-gray-900 mt-1">{email}</p>
                  </div>

                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                    <p className="text-sm text-blue-800">
                      <strong>Didn't receive the email?</strong>
                      <br />
                      Check your spam folder or try again with a different email address.
                    </p>
                  </div>

                  <Button
                    type="button"
                    variant="outline"
                    className="w-full"
                    onClick={() => {
                      setIsEmailSent(false);
                      setEmail('');
                    }}
                  >
                    Try Another Email
                  </Button>
                </>
              )}

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
