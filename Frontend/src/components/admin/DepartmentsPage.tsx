import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit2, FiTrash2 } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { departmentAPI } from '../../services/api';
import { Modal } from '../common/Modal';
import { getErrorMessage } from '../../utils/errorHandler';

export const DepartmentsPage: React.FC = () => {
  const [departments, setDepartments] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingDept, setEditingDept] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    headOfDepartment: '',
  });
  const [errors, setErrors] = useState<any>({});

  useEffect(() => {
    fetchDepartments();
  }, []);

  const fetchDepartments = async () => {
    try {
      const response = await departmentAPI.getAll();
      setDepartments(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      toast.error('Failed to fetch departments');
      setDepartments([]);
    }
  };

  const handleEdit = (dept: any) => {
    setEditingDept(dept);
    setIsEditMode(true);
    setFormData({
      name: dept.name,
      code: dept.code,
      headOfDepartment: dept.headOfDepartment || '',
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({ name: '', code: '', headOfDepartment: '' });
    setEditingDept(null);
    setIsEditMode(false);
    setErrors({});
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.name) newErrors.name = 'Name is required';
    if (!formData.code) newErrors.code = 'Code is required';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      if (isEditMode && editingDept) {
        await departmentAPI.update(editingDept.id, formData);
        toast.success('Department updated successfully!');
      } else {
        await departmentAPI.create(formData);
        toast.success('Department created successfully!');
      }
      setShowModal(false);
      resetForm();
      fetchDepartments();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Department operation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete ${name}?`)) {
      return;
    }

    try {
      await departmentAPI.delete(id);
      toast.success('Department deleted successfully!');
      fetchDepartments();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Department deletion error:', error);
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Departments</h1>
          <p className="text-gray-600 mt-1">Manage college departments</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Department
        </Button>
      </div>

      {/* Departments Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {departments.map((dept) => (
          <Card key={dept.id} hover>
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-bold text-gray-900">{dept.name}</h3>
                <p className="text-sm text-gray-600">Code: {dept.code}</p>
              </div>
              <div className="flex gap-2">
                <button 
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                  onClick={() => handleEdit(dept)}
                  title="Edit"
                >
                  <FiEdit2 size={18} />
                </button>
                <button 
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                  onClick={() => handleDelete(dept.id, dept.name)}
                >
                  <FiTrash2 size={18} />
                </button>
              </div>
            </div>
            {dept.headOfDepartment && (
              <p className="text-sm text-gray-600">
                <span className="font-medium">HOD:</span> {dept.headOfDepartment}
              </p>
            )}
          </Card>
        ))}
      </div>

      {/* Add/Edit Department Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Department" : "Add New Department"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Department Name"
            value={formData.name}
            onChange={(e) => {
              setFormData({ ...formData, name: e.target.value });
              setErrors({ ...errors, name: '' });
            }}
            error={errors.name}
            placeholder="Computer Science"
          />

          <Input
            label="Department Code"
            value={formData.code}
            onChange={(e) => {
              setFormData({ ...formData, code: e.target.value });
              setErrors({ ...errors, code: '' });
            }}
            error={errors.code}
            placeholder="CS"
          />

          <Input
            label="Head of Department"
            value={formData.headOfDepartment}
            onChange={(e) =>
              setFormData({ ...formData, headOfDepartment: e.target.value })
            }
            placeholder="Name"
          />

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
              {isEditMode ? 'Update Department' : 'Create Department'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
