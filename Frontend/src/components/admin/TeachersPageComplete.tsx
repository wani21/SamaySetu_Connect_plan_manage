import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit2, FiTrash2, FiMail, FiPhone } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { Modal } from '../common/Modal';
import { teacherAdminAPI } from '../../services/api';
import { getErrorMessage } from '../../utils/errorHandler';

export const TeachersPageComplete: React.FC = () => {
  const [teachers, setTeachers] = useState<any[]>([]);
  const [pendingTeachers, setPendingTeachers] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'approved' | 'pending'>('approved');
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingTeacher, setEditingTeacher] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    employeeId: '',
    email: '',
    phone: '',
    password: '',
    specialization: '',
    weeklyHoursLimit: '25',
  });
  const [errors, setErrors] = useState<any>({});

  useEffect(() => {
    fetchTeachers();
    fetchPendingApprovals();
  }, []);

  const fetchTeachers = async () => {
    try {
      const response = await teacherAdminAPI.getAll();
      const allUsers = Array.isArray(response.data) ? response.data : [];
      // Filter to show only approved teachers (role === 'TEACHER' and isApproved === true)
      const approvedTeachers = allUsers.filter((user: any) => 
        user.role === 'TEACHER' && user.isApproved === true
      );
      setTeachers(approvedTeachers);
    } catch (error: any) {
      console.error('Failed to fetch teachers:', error);
      toast.error('Failed to fetch teachers');
      setTeachers([]);
    }
  };

  const fetchPendingApprovals = async () => {
    try {
      const response = await teacherAdminAPI.getPendingApprovals();
      setPendingTeachers(Array.isArray(response.data) ? response.data : []);
    } catch (error: any) {
      console.error('Failed to fetch pending approvals:', error);
      setPendingTeachers([]);
    }
  };

  const handleApprove = async (id: number, name: string) => {
    if (!window.confirm(`Approve ${name}'s account?`)) {
      return;
    }

    try {
      await teacherAdminAPI.approve(id);
      toast.success(`${name} approved successfully!`);
      fetchTeachers();
      fetchPendingApprovals();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Approval error:', error);
    }
  };

  const handleReject = async (id: number, name: string) => {
    const reason = window.prompt(`Reject ${name}'s account?\n\nOptional: Enter reason for rejection:`);
    if (reason === null) return; // User cancelled

    try {
      await teacherAdminAPI.reject(id, reason || 'Application rejected by administrator');
      toast.success(`${name}'s application rejected`);
      fetchPendingApprovals();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Rejection error:', error);
    }
  };

  const handleEdit = (teacher: any) => {
    setEditingTeacher(teacher);
    setIsEditMode(true);
    setFormData({
      name: teacher.name,
      employeeId: teacher.employeeId,
      email: teacher.email,
      phone: teacher.phone || '',
      password: '', // Don't populate password for security
      specialization: teacher.specialization || '',
      weeklyHoursLimit: teacher.weeklyHoursLimit?.toString() || '25',
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      name: '',
      employeeId: '',
      email: '',
      phone: '',
      password: '',
      specialization: '',
      weeklyHoursLimit: '25',
    });
    setEditingTeacher(null);
    setIsEditMode(false);
    setErrors({});
  };

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete ${name}?`)) {
      return;
    }

    try {
      await teacherAdminAPI.delete(id);
      toast.success('Teacher deleted successfully!');
      fetchTeachers();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Teacher deletion error:', error);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.name) newErrors.name = 'Name is required';
    if (!formData.employeeId) newErrors.employeeId = 'Employee ID is required';
    if (!formData.email) newErrors.email = 'Email is required';
    // Password is only required when creating, not editing
    if (!isEditMode && !formData.password) newErrors.password = 'Password is required';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      const teacherData: any = {
        name: formData.name,
        employeeId: formData.employeeId,
        email: formData.email,
        phone: formData.phone || null,
        specialization: formData.specialization || null,
        weeklyHoursLimit: parseInt(formData.weeklyHoursLimit) || 25,
      };

      // Only include password if it's provided (for create or password change)
      if (formData.password) {
        teacherData.password = formData.password;
      }

      if (isEditMode && editingTeacher) {
        await teacherAdminAPI.update(editingTeacher.id, teacherData);
        toast.success('Teacher updated successfully!');
      } else {
        // For create, password is required (already validated above)
        teacherData.password = formData.password;
        await teacherAdminAPI.create(teacherData);
        toast.success('Teacher created successfully!');
      }

      setShowModal(false);
      resetForm();
      fetchTeachers();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Teacher operation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Teachers</h1>
          <p className="text-gray-600 mt-1">Manage teaching staff and approvals</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Teacher
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex gap-4 mb-6 border-b border-gray-200">
        <button
          onClick={() => setActiveTab('approved')}
          className={`pb-3 px-4 font-medium transition-colors ${
            activeTab === 'approved'
              ? 'text-primary-600 border-b-2 border-primary-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Approved Teachers ({teachers.length})
        </button>
        <button
          onClick={() => setActiveTab('pending')}
          className={`pb-3 px-4 font-medium relative transition-colors ${
            activeTab === 'pending'
              ? 'text-primary-600 border-b-2 border-primary-600'
              : 'text-gray-600 hover:text-gray-900'
          }`}
        >
          Pending Approvals
          {pendingTeachers.length > 0 && (
            <span className="ml-2 px-2 py-0.5 bg-orange-500 text-white text-xs rounded-full">
              {pendingTeachers.length}
            </span>
          )}
        </button>
      </div>

      {/* Approved Teachers Table */}
      {activeTab === 'approved' && (
        <Card>
          {teachers.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-500 text-lg">No approved teachers yet</p>
              <p className="text-gray-400 text-sm mt-2">Add teachers manually or approve pending registrations</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Name</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Employee ID</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Email</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Phone</th>
                    <th className="text-left py-3 px-4 font-semibold text-gray-700">Specialization</th>
                    <th className="text-right py-3 px-4 font-semibold text-gray-700">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {teachers.map((teacher) => (
                    <tr key={teacher.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-3 px-4 font-medium text-gray-900">{teacher.name}</td>
                      <td className="py-3 px-4 text-gray-600">{teacher.employeeId}</td>
                      <td className="py-3 px-4 text-gray-600">
                        <div className="flex items-center gap-2">
                          <FiMail size={14} />
                          {teacher.email}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-gray-600">
                        <div className="flex items-center gap-2">
                          <FiPhone size={14} />
                          {teacher.phone || '-'}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-gray-600">{teacher.specialization || '-'}</td>
                      <td className="py-3 px-4">
                        <div className="flex justify-end gap-2">
                          <button 
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                            onClick={() => handleEdit(teacher)}
                            title="Edit"
                          >
                            <FiEdit2 size={18} />
                          </button>
                          <button 
                            className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                            onClick={() => handleDelete(teacher.id, teacher.name)}
                            title="Delete"
                          >
                            <FiTrash2 size={18} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      )}

      {/* Pending Approvals */}
      {activeTab === 'pending' && (
        <div>
          {pendingTeachers.length === 0 ? (
            <Card>
              <div className="text-center py-12">
                <p className="text-gray-500 text-lg">No pending approvals</p>
                <p className="text-gray-400 text-sm mt-2">All teacher registrations have been processed</p>
              </div>
            </Card>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {pendingTeachers.map((teacher) => (
                <Card key={teacher.id} className="border-l-4 border-orange-500">
                  <div className="space-y-3">
                    <div className="flex items-start justify-between">
                      <div>
                        <h3 className="text-lg font-bold text-gray-900">{teacher.name}</h3>
                        <span className="inline-block mt-1 px-2 py-1 bg-orange-100 text-orange-800 text-xs rounded-full">
                          ⏳ Pending Approval
                        </span>
                      </div>
                    </div>
                    
                    <div className="space-y-2 text-sm">
                      <div className="flex items-center gap-2 text-gray-600">
                        <span className="font-medium">Employee ID:</span>
                        <span>{teacher.employeeId}</span>
                      </div>
                      <div className="flex items-center gap-2 text-gray-600">
                        <FiMail size={14} />
                        <span className="truncate">{teacher.email}</span>
                      </div>
                      {teacher.phone && (
                        <div className="flex items-center gap-2 text-gray-600">
                          <FiPhone size={14} />
                          <span>{teacher.phone}</span>
                        </div>
                      )}
                      {teacher.specialization && (
                        <div className="text-gray-600">
                          <span className="font-medium">Specialization:</span>
                          <p className="mt-1">{teacher.specialization}</p>
                        </div>
                      )}
                      <div className="text-gray-500 text-xs pt-2">
                        Registered: {new Date(teacher.createdAt).toLocaleDateString()}
                      </div>
                    </div>

                    <div className="flex gap-2 pt-3 border-t border-gray-200">
                      <Button
                        variant="primary"
                        onClick={() => handleApprove(teacher.id, teacher.name)}
                        className="flex-1 flex items-center justify-center gap-2"
                      >
                        ✓ Approve
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => handleReject(teacher.id, teacher.name)}
                        className="flex-1 flex items-center justify-center gap-2 text-red-600 border-red-600 hover:bg-red-50"
                      >
                        ✗ Reject
                      </Button>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Add/Edit Teacher Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Teacher" : "Add New Teacher"}
        size="lg"
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Full Name"
              value={formData.name}
              onChange={(e) => {
                setFormData({ ...formData, name: e.target.value });
                setErrors({ ...errors, name: '' });
              }}
              error={errors.name}
              placeholder="Dr. John Smith"
            />

            <Input
              label="Employee ID"
              value={formData.employeeId}
              onChange={(e) => {
                setFormData({ ...formData, employeeId: e.target.value });
                setErrors({ ...errors, employeeId: '' });
              }}
              error={errors.employeeId}
              placeholder="EMP001"
            />
          </div>

          <Input
            label="Email"
            type="email"
            value={formData.email}
            onChange={(e) => {
              setFormData({ ...formData, email: e.target.value });
              setErrors({ ...errors, email: '' });
            }}
            error={errors.email}
            placeholder="teacher@mitaoe.ac.in"
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="Phone"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              placeholder="1234567890"
            />

            <Input
              label="Weekly Hours Limit"
              type="number"
              value={formData.weeklyHoursLimit}
              onChange={(e) => setFormData({ ...formData, weeklyHoursLimit: e.target.value })}
            />
          </div>

          <Input
            label="Specialization"
            value={formData.specialization}
            onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
            placeholder="Data Structures, Algorithms"
          />

          {!isEditMode && (
            <Input
              label="Password"
              type="password"
              value={formData.password}
              onChange={(e) => {
                setFormData({ ...formData, password: e.target.value });
                setErrors({ ...errors, password: '' });
              }}
              error={errors.password}
              placeholder="Temporary password"
            />
          )}

          <div className="flex gap-3 pt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => setShowModal(false)}
              className="flex-1"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              className="flex-1"
            >
              {isEditMode ? 'Update Teacher' : 'Create Teacher'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
