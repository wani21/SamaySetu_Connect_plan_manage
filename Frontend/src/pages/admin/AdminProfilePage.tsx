import React, { useState, useEffect } from 'react';
import { FiUser, FiMail, FiPhone, FiBriefcase, FiSave } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Input } from '../../components/common/Input';
import { useAuthStore } from '../../store/authStore';
import { teacherAPI } from '../../services/api';
import { useDashboardStats } from '../../hooks/useDashboardStats';

export const AdminProfilePage: React.FC = () => {
  const { user } = useAuthStore();
  const { stats, isLoading: statsLoading } = useDashboardStats();
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isFetching, setIsFetching] = useState(true);
  const [profileData, setProfileData] = useState<any>(null);
  const [formData, setFormData] = useState({
    name: '',
    email: user?.email || '',
    phone: '',
    specialization: '',
    employeeId: '',
  });

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      setIsFetching(true);
      const response = await teacherAPI.getProfile();
      const data = response.data;
      setProfileData(data);
      setFormData({
        name: data.name || '',
        email: data.email || '',
        phone: data.phone || '',
        specialization: data.specialization || '',
        employeeId: data.employeeId || '',
      });
    } catch (error: any) {
      console.error('Error fetching profile:', error);
      toast.error('Failed to load profile data');
    } finally {
      setIsFetching(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const updateData = {
        name: formData.name,
        phone: formData.phone,
        specialization: formData.specialization,
        email: formData.email,
        employeeId: formData.employeeId,
        weeklyHoursLimit: profileData?.weeklyHoursLimit || 25,
        departmentId: profileData?.department?.id || null,
        // Don't include password unless changing it
      };
      
      await teacherAPI.updateProfile(updateData);
      toast.success('Profile updated successfully!');
      setIsEditing(false);
      await fetchProfile();
    } catch (error: any) {
      console.error('Error updating profile:', error);
      const message = error.response?.data?.message || error.response?.data || 'Failed to update profile';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const getUserName = () => {
    if (profileData?.name) return profileData.name;
    if (formData.name) return formData.name;
    if (user?.name) return user.name;
    if (user?.email) {
      const emailName = user.email.split('@')[0];
      return emailName.replace(/\./g, ' ').replace(/\d+/g, '').trim();
    }
    return 'Admin';
  };

  const formatName = (name: string) => {
    return name
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  if (isFetching) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading profile...</p>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">My Profile</h1>
        <p className="text-gray-600 mt-1">Manage your account information</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profile Card */}
        <Card>
          <div className="text-center">
            <div className="w-32 h-32 mx-auto rounded-full bg-gradient-to-br from-primary-600 to-primary-800 flex items-center justify-center text-white font-bold text-4xl mb-4">
              {getUserName().charAt(0).toUpperCase()}
            </div>
            <h2 className="text-2xl font-bold text-gray-900">{formatName(getUserName())}</h2>
            <p className="text-gray-600 capitalize mt-1">{user?.role}</p>
            <p className="text-sm text-gray-500 mt-2">{user?.email}</p>
            
            <div className="mt-6 pt-6 border-t border-gray-200">
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">Status:</span>
                  <span className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium">
                    Active
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Role:</span>
                  <span className="font-medium text-gray-900 capitalize">{user?.role}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Member Since:</span>
                  <span className="font-medium text-gray-900">
                    {profileData?.createdAt 
                      ? new Date(profileData.createdAt).getFullYear()
                      : new Date().getFullYear()}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </Card>

        {/* Profile Information */}
        <Card className="lg:col-span-2">
          <div className="flex justify-between items-center mb-6">
            <h3 className="text-xl font-bold text-gray-900">Profile Information</h3>
            {!isEditing && (
              <Button
                variant="outline"
                onClick={() => setIsEditing(true)}
                className="flex items-center gap-2"
              >
                <FiUser size={16} />
                Edit Profile
              </Button>
            )}
          </div>

          {isEditing ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                label="Full Name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                icon={<FiUser />}
                placeholder="John Doe"
              />

              <Input
                label="Email Address"
                type="email"
                value={formData.email}
                disabled
                icon={<FiMail />}
                placeholder="admin@mitaoe.ac.in"
              />

              <Input
                label="Phone Number"
                type="tel"
                value={formData.phone}
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                icon={<FiPhone />}
                placeholder="9876543210"
              />

              <Input
                label="Specialization"
                value={formData.specialization}
                onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
                icon={<FiBriefcase />}
                placeholder="System Administration"
              />

              <div className="flex gap-3 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setIsEditing(false)}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  className="flex-1 flex items-center justify-center gap-2"
                  isLoading={isLoading}
                >
                  <FiSave size={16} />
                  Save Changes
                </Button>
              </div>
            </form>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                <FiUser className="text-gray-400" size={20} />
                <div>
                  <p className="text-sm text-gray-600">Full Name</p>
                  <p className="font-medium text-gray-900">{formatName(getUserName())}</p>
                </div>
              </div>

              <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                <FiMail className="text-gray-400" size={20} />
                <div>
                  <p className="text-sm text-gray-600">Email Address</p>
                  <p className="font-medium text-gray-900">{user?.email}</p>
                </div>
              </div>

              <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                <FiPhone className="text-gray-400" size={20} />
                <div>
                  <p className="text-sm text-gray-600">Phone Number</p>
                  <p className="font-medium text-gray-900">{formData.phone || 'Not provided'}</p>
                </div>
              </div>

              <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                <FiBriefcase className="text-gray-400" size={20} />
                <div>
                  <p className="text-sm text-gray-600">Specialization</p>
                  <p className="font-medium text-gray-900">{formData.specialization || 'Not provided'}</p>
                </div>
              </div>
            </div>
          )}
        </Card>
      </div>

      {/* Additional Information */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
        <Card>
          <div className="text-center">
            <div className="text-3xl font-bold text-primary-600 mb-2">
              {statsLoading ? '...' : stats.totalTeachers}
            </div>
            <p className="text-sm text-gray-600">Teachers Managed</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <div className="text-3xl font-bold text-green-600 mb-2">
              {statsLoading ? '...' : stats.totalCourses}
            </div>
            <p className="text-sm text-gray-600">Courses Created</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <div className="text-3xl font-bold text-purple-600 mb-2">
              {statsLoading ? '...' : stats.totalDepartments}
            </div>
            <p className="text-sm text-gray-600">Departments</p>
          </div>
        </Card>
      </div>
    </div>
  );
};
