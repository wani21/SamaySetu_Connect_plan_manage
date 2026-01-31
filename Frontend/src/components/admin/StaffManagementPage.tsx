import React, { useState, useEffect, useRef } from 'react';
import { FiUpload, FiDownload, FiPlus, FiEdit2, FiTrash2, FiUsers, FiCheck, FiX, FiSearch } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { Modal } from '../common/Modal';
import { adminAPI, teacherAdminAPI, departmentAPI } from '../../services/api';
import { getErrorMessage } from '../../utils/errorHandler';

export const StaffManagementPage: React.FC = () => {
  const [staff, setStaff] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'list' | 'upload' | 'add'>('list');
  const [searchTerm, setSearchTerm] = useState('');
  const [filterDepartment, setFilterDepartment] = useState('');
  
  // Upload state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadResult, setUploadResult] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  // Manual add/edit state
  const [showModal, setShowModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    employeeId: '',
    email: '',
    phone: '',
    specialization: '',
    minWeeklyHours: '10',
    maxWeeklyHours: '30',
    departmentId: '',
  });
  const [errors, setErrors] = useState<any>({});

  useEffect(() => {
    fetchStaff();
    fetchDepartments();
  }, []);

  const fetchStaff = async () => {
    try {
      const response = await teacherAdminAPI.getAll();
      setStaff(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      toast.error('Failed to fetch staff');
      setStaff([]);
    }
  };

  const fetchDepartments = async () => {
    try {
      const response = await departmentAPI.getAll();
      setDepartments(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      setDepartments([]);
    }
  };

  // CSV Upload handlers
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.toLowerCase().endsWith('.csv')) {
        toast.error('Please select a CSV file');
        return;
      }
      setSelectedFile(file);
      setUploadResult(null);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      toast.error('Please select a file first');
      return;
    }

    setIsLoading(true);
    try {
      const response = await adminAPI.uploadStaff(selectedFile);
      setUploadResult(response.data);
      toast.success(response.data);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      fetchStaff();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage);
      setUploadResult(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const response = await adminAPI.downloadStaffTemplate();
      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'staff_template.csv';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success('Template downloaded successfully');
    } catch (error) {
      toast.error('Failed to download template');
    }
  };

  // Manual add/edit handlers
  const resetForm = () => {
    setFormData({
      name: '',
      employeeId: '',
      email: '',
      phone: '',
      specialization: '',
      minWeeklyHours: '10',
      maxWeeklyHours: '30',
      departmentId: '',
    });
    setEditingStaff(null);
    setIsEditMode(false);
    setErrors({});
  };

  const handleEdit = (staffMember: any) => {
    setEditingStaff(staffMember);
    setIsEditMode(true);
    setFormData({
      name: staffMember.name || '',
      employeeId: staffMember.employeeId || '',
      email: staffMember.email || '',
      phone: staffMember.phone || '',
      specialization: staffMember.specialization || '',
      minWeeklyHours: staffMember.minWeeklyHours?.toString() || '10',
      maxWeeklyHours: staffMember.maxWeeklyHours?.toString() || '30',
      departmentId: staffMember.department?.id?.toString() || '',
    });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.name) newErrors.name = 'Name is required';
    if (!formData.employeeId) newErrors.employeeId = 'Employee ID is required';
    if (!formData.email) newErrors.email = 'Email is required';
    else if (!formData.email.endsWith('@mitaoe.ac.in')) newErrors.email = 'Must be a college email';
    if (!formData.minWeeklyHours) newErrors.minWeeklyHours = 'Min hours required';
    if (!formData.maxWeeklyHours) newErrors.maxWeeklyHours = 'Max hours required';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      if (isEditMode && editingStaff) {
        const updateData = {
          name: formData.name,
          employeeId: formData.employeeId,
          email: formData.email,
          phone: formData.phone,
          specialization: formData.specialization,
          minWeeklyHours: parseInt(formData.minWeeklyHours),
          maxWeeklyHours: parseInt(formData.maxWeeklyHours),
          departmentId: formData.departmentId ? parseInt(formData.departmentId) : null,
        };
        await adminAPI.updateStaff(editingStaff.id, updateData);
        toast.success('Staff updated successfully!');
      } else {
        await adminAPI.createStaffManually({
          name: formData.name,
          employeeId: formData.employeeId,
          email: formData.email,
          phone: formData.phone,
          specialization: formData.specialization,
          minWeeklyHours: parseInt(formData.minWeeklyHours),
          maxWeeklyHours: parseInt(formData.maxWeeklyHours),
        });
        toast.success('Staff created successfully!');
      }
      setShowModal(false);
      resetForm();
      fetchStaff();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete ${name}?`)) return;
    
    try {
      await teacherAdminAPI.delete(id);
      toast.success('Staff deleted successfully!');
      fetchStaff();
    } catch (error: any) {
      toast.error(getErrorMessage(error));
    }
  };

  // Filter staff
  const filteredStaff = staff.filter((s) => {
    const matchesSearch = searchTerm === '' || 
      s.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.employeeId?.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesDepartment = filterDepartment === '' || 
      s.department?.id?.toString() === filterDepartment;
    
    return matchesSearch && matchesDepartment;
  });

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Staff Management</h1>
          <p className="text-gray-600 mt-1">Manage teaching staff and their details</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Staff
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6 border-b border-gray-200">
        <button
          onClick={() => setActiveTab('list')}
          className={`px-4 py-2 font-medium border-b-2 transition-colors ${
            activeTab === 'list' 
              ? 'border-primary-600 text-primary-600' 
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <FiUsers className="inline mr-2" />
          Staff List ({staff.length})
        </button>
        <button
          onClick={() => setActiveTab('upload')}
          className={`px-4 py-2 font-medium border-b-2 transition-colors ${
            activeTab === 'upload' 
              ? 'border-primary-600 text-primary-600' 
              : 'border-transparent text-gray-500 hover:text-gray-700'
          }`}
        >
          <FiUpload className="inline mr-2" />
          Bulk Registration
        </button>
      </div>

      {/* Staff List Tab */}
      {activeTab === 'list' && (
        <>
          {/* Search and Filter */}
          <Card className="mb-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-2">
                <div className="relative">
                  <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Search by name, email, or employee ID..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="input-field pl-10"
                  />
                </div>
              </div>
              <div>
                <select
                  value={filterDepartment}
                  onChange={(e) => setFilterDepartment(e.target.value)}
                  className="input-field"
                >
                  <option value="">All Departments</option>
                  {departments.map((dept) => (
                    <option key={dept.id} value={dept.id}>{dept.name}</option>
                  ))}
                </select>
              </div>
            </div>
          </Card>

          {/* Staff Table */}
          <Card>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="text-left py-3 px-4 font-medium text-gray-700">Name</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-700">Employee ID</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-700">Email</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-700">Department</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-700">Weekly Hours</th>
                    <th className="text-left py-3 px-4 font-medium text-gray-700">Status</th>
                    <th className="text-right py-3 px-4 font-medium text-gray-700">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStaff.map((s) => (
                    <tr key={s.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-3 px-4">
                        <div className="font-medium text-gray-900">{s.name}</div>
                        <div className="text-sm text-gray-500">{s.specialization}</div>
                      </td>
                      <td className="py-3 px-4 text-gray-600">{s.employeeId}</td>
                      <td className="py-3 px-4 text-gray-600">{s.email}</td>
                      <td className="py-3 px-4 text-gray-600">{s.department?.name || '-'}</td>
                      <td className="py-3 px-4 text-gray-600">{s.minWeeklyHours}-{s.maxWeeklyHours}</td>
                      <td className="py-3 px-4">
                        <div className="flex gap-1">
                          {s.isActive && (
                            <span className="px-2 py-0.5 bg-green-100 text-green-800 rounded text-xs">Active</span>
                          )}
                          {s.isEmailVerified && (
                            <span className="px-2 py-0.5 bg-blue-100 text-blue-800 rounded text-xs">Verified</span>
                          )}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => handleEdit(s)}
                          className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                          title="Edit"
                        >
                          <FiEdit2 size={16} />
                        </button>
                        <button
                          onClick={() => handleDelete(s.id, s.name)}
                          className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                          title="Delete"
                        >
                          <FiTrash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {filteredStaff.length === 0 && (
                <div className="text-center py-12 text-gray-500">
                  {staff.length === 0 ? 'No staff members found. Add staff using the button above or Bulk Registration.' : 'No staff match your search criteria.'}
                </div>
              )}
            </div>
          </Card>
        </>
      )}

      {/* Bulk Registration Tab */}
      {activeTab === 'upload' && (
        <Card>
          <div className="max-w-2xl mx-auto py-8">
            <div className="text-center mb-8">
              <div className="mx-auto w-16 h-16 bg-primary-100 rounded-full flex items-center justify-center mb-4">
                <FiUpload className="w-8 h-8 text-primary-600" />
              </div>
              <h2 className="text-xl font-bold text-gray-900 mb-2">Bulk Staff Registration</h2>
              <p className="text-gray-600">Upload a CSV file to add multiple staff members at once</p>
            </div>

            {/* Download Template */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-blue-900">Download CSV Template</p>
                  <p className="text-sm text-blue-700">Use this template to format your staff data correctly</p>
                </div>
                <Button
                  variant="outline"
                  onClick={handleDownloadTemplate}
                  className="flex items-center gap-2"
                >
                  <FiDownload /> Download
                </Button>
              </div>
            </div>

            {/* File Upload */}
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center mb-6">
              <input
                type="file"
                accept=".csv"
                onChange={handleFileSelect}
                ref={fileInputRef}
                className="hidden"
                id="csv-upload"
              />
              <label htmlFor="csv-upload" className="cursor-pointer">
                <FiUpload className="mx-auto w-12 h-12 text-gray-400 mb-4" />
                <p className="text-gray-600 mb-2">
                  {selectedFile ? selectedFile.name : 'Click to select or drag and drop'}
                </p>
                <p className="text-sm text-gray-500">CSV files only</p>
              </label>
            </div>

            {selectedFile && (
              <div className="flex items-center justify-between bg-gray-50 rounded-lg p-4 mb-6">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                    <FiCheck className="text-green-600" />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900">{selectedFile.name}</p>
                    <p className="text-sm text-gray-500">{(selectedFile.size / 1024).toFixed(2)} KB</p>
                  </div>
                </div>
                <button
                  onClick={() => {
                    setSelectedFile(null);
                    if (fileInputRef.current) fileInputRef.current.value = '';
                  }}
                  className="p-2 text-gray-400 hover:text-gray-600"
                >
                  <FiX />
                </button>
              </div>
            )}

            <Button
              variant="primary"
              onClick={handleUpload}
              isLoading={isLoading}
              disabled={!selectedFile}
              className="w-full"
            >
              Upload Staff
            </Button>

            {uploadResult && (
              <div className={`mt-4 p-4 rounded-lg ${
                uploadResult.includes('Successfully') 
                  ? 'bg-green-50 text-green-800 border border-green-200' 
                  : 'bg-red-50 text-red-800 border border-red-200'
              }`}>
                {uploadResult}
              </div>
            )}

            {/* Instructions */}
            <div className="mt-8 bg-gray-50 rounded-lg p-4">
              <h3 className="font-medium text-gray-900 mb-2">CSV Format Instructions</h3>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• First row should be headers: Name, Employee ID, Email, Phone, Specialization, Min Weekly Hours, Max Weekly Hours</li>
                <li>• Email must be a valid college email (@mitaoe.ac.in)</li>
                <li>• Employee ID must be unique</li>
                <li>• Default password will be set to: mitaoe@123</li>
                <li>• Staff will be required to change password on first login</li>
              </ul>
            </div>
          </div>
        </Card>
      )}

      {/* Add/Edit Staff Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Staff" : "Add New Staff"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Full Name"
              value={formData.name}
              onChange={(e) => {
                setFormData({ ...formData, name: e.target.value });
                setErrors({ ...errors, name: '' });
              }}
              error={errors.name}
              placeholder="John Doe"
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
            placeholder="name@mitaoe.ac.in"
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Phone"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              placeholder="9876543210"
            />
            <Input
              label="Specialization"
              value={formData.specialization}
              onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
              placeholder="Computer Science"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Min Weekly Hours"
              type="number"
              value={formData.minWeeklyHours}
              onChange={(e) => {
                setFormData({ ...formData, minWeeklyHours: e.target.value });
                setErrors({ ...errors, minWeeklyHours: '' });
              }}
              error={errors.minWeeklyHours}
              min="1"
            />
            <Input
              label="Max Weekly Hours"
              type="number"
              value={formData.maxWeeklyHours}
              onChange={(e) => {
                setFormData({ ...formData, maxWeeklyHours: e.target.value });
                setErrors({ ...errors, maxWeeklyHours: '' });
              }}
              error={errors.maxWeeklyHours}
              min="1"
            />
          </div>

          {isEditMode && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Department</label>
              <select
                value={formData.departmentId}
                onChange={(e) => setFormData({ ...formData, departmentId: e.target.value })}
                className="input-field"
              >
                <option value="">Select Department</option>
                {departments.map((dept) => (
                  <option key={dept.id} value={dept.id}>{dept.name}</option>
                ))}
              </select>
            </div>
          )}

          {!isEditMode && (
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-800">
              <p><strong>Note:</strong> Default password will be set to <code className="bg-blue-100 px-1 rounded">mitaoe@123</code></p>
              <p>Staff will be required to change password on first login.</p>
            </div>
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
              {isEditMode ? 'Update Staff' : 'Create Staff'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
