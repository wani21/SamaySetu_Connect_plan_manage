import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit2, FiTrash2, FiCalendar, FiCheckCircle } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { academicYearAPI } from '../../services/api';
import { Modal } from '../common/Modal';
import { getErrorMessage } from '../../utils/errorHandler';

export const AcademicYearsPage: React.FC = () => {
  const [academicYears, setAcademicYears] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingYear, setEditingYear] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    yearName: '',
    startDate: '',
    endDate: '',
    isCurrent: false,
  });
  const [errors, setErrors] = useState<any>({});

  useEffect(() => {
    fetchAcademicYears();
  }, []);

  const fetchAcademicYears = async () => {
    try {
      const response = await academicYearAPI.getAll();
      console.log('Academic Years API Response:', response);
      console.log('Academic Years Data:', response.data);
      console.log('Is Array?', Array.isArray(response.data));
      setAcademicYears(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Failed to fetch academic years:', error);
      toast.error('Failed to fetch academic years');
      setAcademicYears([]); // Set empty array on error
    }
  };

  const handleEdit = (year: any) => {
    setEditingYear(year);
    setIsEditMode(true);
    setFormData({
      yearName: year.yearName,
      startDate: year.startDate.split('T')[0],
      endDate: year.endDate.split('T')[0],
      isCurrent: year.isCurrent,
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({ yearName: '', startDate: '', endDate: '', isCurrent: false });
    setEditingYear(null);
    setIsEditMode(false);
    setErrors({});
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.yearName) newErrors.yearName = 'Year name is required';
    if (!formData.startDate) newErrors.startDate = 'Start date is required';
    if (!formData.endDate) newErrors.endDate = 'End date is required';
    
    if (formData.startDate && formData.endDate && formData.startDate >= formData.endDate) {
      newErrors.endDate = 'End date must be after start date';
    }
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      if (isEditMode && editingYear) {
        await academicYearAPI.update(editingYear.id, formData);
        toast.success('Academic Year updated successfully!');
      } else {
        await academicYearAPI.create(formData);
        toast.success('Academic Year created successfully!');
      }
      setShowModal(false);
      resetForm();
      fetchAcademicYears();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Academic year operation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number, yearName: string) => {
    if (!window.confirm(`Are you sure you want to delete academic year ${yearName}?`)) {
      return;
    }

    try {
      await academicYearAPI.delete(id);
      toast.success('Academic year deleted successfully!');
      fetchAcademicYears();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Academic year deletion error:', error);
    }
  };

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Academic Years</h1>
          <p className="text-gray-600 mt-1">Manage academic years and timetable periods</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Academic Year
        </Button>
      </div>

      {/* Academic Years Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {academicYears.length === 0 ? (
          <div className="col-span-full text-center py-12 text-gray-500">
            No academic years found. Create your first academic year to get started.
          </div>
        ) : (
          academicYears.map((year) => (
          <Card key={year.id} hover className={year.isCurrent ? 'border-2 border-green-500' : ''}>
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-center gap-2">
                <FiCalendar className="text-primary-600" size={24} />
                <div>
                  <h3 className="text-xl font-bold text-gray-900">{year.yearName}</h3>
                  {year.isCurrent && (
                    <span className="inline-flex items-center gap-1 text-xs bg-green-100 text-green-800 px-2 py-1 rounded-full mt-1">
                      <FiCheckCircle size={12} /> Current Year
                    </span>
                  )}
                </div>
              </div>
              <div className="flex gap-2">
                <button 
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                  onClick={() => handleEdit(year)}
                  title="Edit"
                >
                  <FiEdit2 size={18} />
                </button>
                <button 
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                  onClick={() => handleDelete(year.id, year.yearName)}
                >
                  <FiTrash2 size={18} />
                </button>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <p className="text-gray-600">
                <span className="font-medium">Start:</span> {new Date(year.startDate).toLocaleDateString()}
              </p>
              <p className="text-gray-600">
                <span className="font-medium">End:</span> {new Date(year.endDate).toLocaleDateString()}
              </p>
              <p className="text-gray-600">
                <span className="font-medium">Duration:</span> {Math.ceil((new Date(year.endDate).getTime() - new Date(year.startDate).getTime()) / (1000 * 60 * 60 * 24 * 30))} months
              </p>
            </div>
          </Card>
          ))
        )}
      </div>

      {/* Add/Edit Academic Year Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Academic Year" : "Add New Academic Year"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Year Name"
            value={formData.yearName}
            onChange={(e) => {
              setFormData({ ...formData, yearName: e.target.value });
              setErrors({ ...errors, yearName: '' });
            }}
            error={errors.yearName}
            placeholder="2025-26"
          />

          <Input
            label="Start Date"
            type="date"
            value={formData.startDate}
            onChange={(e) => {
              setFormData({ ...formData, startDate: e.target.value });
              setErrors({ ...errors, startDate: '' });
            }}
            error={errors.startDate}
          />

          <Input
            label="End Date"
            type="date"
            value={formData.endDate}
            onChange={(e) => {
              setFormData({ ...formData, endDate: e.target.value });
              setErrors({ ...errors, endDate: '' });
            }}
            error={errors.endDate}
          />

          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isCurrent"
                checked={formData.isCurrent}
                onChange={(e) => setFormData({ ...formData, isCurrent: e.target.checked })}
                className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
              />
              <label htmlFor="isCurrent" className="text-sm text-gray-700">
                Set as current academic year
              </label>
            </div>
          </div>

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
              {isEditMode ? 'Update Academic Year' : 'Create Academic Year'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
