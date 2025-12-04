import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit2, FiTrash2, FiUsers } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { divisionAPI, departmentAPI, academicYearAPI } from '../../services/api';
import { Modal } from '../common/Modal';
import { getErrorMessage } from '../../utils/errorHandler';

export const DivisionsPage: React.FC = () => {
  const [divisions, setDivisions] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [academicYears, setAcademicYears] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingDivision, setEditingDivision] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    year: '1',
    branch: '',
    departmentId: '',
    academicYearId: '',
    totalStudents: '',
  });
  const [errors, setErrors] = useState<any>({});
  
  // Filter states
  const [filterYear, setFilterYear] = useState('');
  const [filterDepartment, setFilterDepartment] = useState('');
  const [filterAcademicYear, setFilterAcademicYear] = useState('');

  useEffect(() => {
    fetchDivisions();
    fetchDepartments();
    fetchAcademicYears();
  }, []);

  const fetchDivisions = async () => {
    try {
      const response = await divisionAPI.getAll();
      console.log('Divisions API Response:', response);
      console.log('Divisions Data:', response.data);
      console.log('Is Array?', Array.isArray(response.data));
      setDivisions(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Failed to fetch divisions:', error);
      toast.error('Failed to fetch divisions');
      setDivisions([]); // Set empty array on error
    }
  };

  const fetchDepartments = async () => {
    try {
      const response = await departmentAPI.getAll();
      setDepartments(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      console.error('Failed to fetch departments');
      setDepartments([]);
    }
  };

  const fetchAcademicYears = async () => {
    try {
      const response = await academicYearAPI.getAll();
      const years = Array.isArray(response.data) ? response.data : [];
      setAcademicYears(years);
      
      // Auto-select current academic year if available
      if (years.length > 0 && !formData.academicYearId) {
        const currentYear = years.find(year => year.isCurrent);
        const defaultYearId = currentYear ? currentYear.id : years[0].id;
        setFormData(prev => ({ ...prev, academicYearId: defaultYearId.toString() }));
      }
    } catch (error) {
      console.error('Failed to fetch academic years');
      setAcademicYears([]);
    }
  };

  const handleEdit = (division: any) => {
    setEditingDivision(division);
    setIsEditMode(true);
    setFormData({
      name: division.name,
      year: division.year.toString(),
      branch: division.branch,
      departmentId: division.department.id.toString(),
      academicYearId: division.academicYear.id.toString(),
      totalStudents: division.totalStudents.toString(),
    });
    setShowModal(true);
  };

  const resetForm = () => {
    // Auto-select current academic year or first available
    let defaultAcademicYearId = '';
    if (academicYears.length > 0) {
      const currentYear = academicYears.find(year => year.isCurrent);
      defaultAcademicYearId = currentYear ? currentYear.id.toString() : academicYears[0].id.toString();
    }

    setFormData({
      name: '',
      year: '1',
      branch: '',
      departmentId: '',
      academicYearId: defaultAcademicYearId,
      totalStudents: '',
    });
    setEditingDivision(null);
    setIsEditMode(false);
    setErrors({});
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.name) newErrors.name = 'Division name is required';
    if (!formData.branch) newErrors.branch = 'Branch is required';
    if (!formData.departmentId) newErrors.departmentId = 'Department is required';
    if (!formData.academicYearId) newErrors.academicYearId = 'Academic year is required';
    if (!formData.totalStudents) newErrors.totalStudents = 'Total students is required';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      const divisionData = {
        name: formData.name,
        year: parseInt(formData.year),
        branch: formData.branch,
        totalStudents: parseInt(formData.totalStudents),
        department: {
          id: parseInt(formData.departmentId)
        },
        academicYear: {
          id: parseInt(formData.academicYearId)
        },
      };

      if (isEditMode && editingDivision) {
        await divisionAPI.update(editingDivision.id, divisionData);
        toast.success('Division updated successfully!');
      } else {
        await divisionAPI.create(divisionData);
        toast.success('Division created successfully!');
      }

      setShowModal(false);
      resetForm();
      fetchDivisions();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Division operation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string, year: number) => {
    if (!window.confirm(`Are you sure you want to delete division ${getYearLabel(year).split(' ')[0]}-${name}?`)) {
      return;
    }

    try {
      await divisionAPI.delete(id);
      toast.success('Division deleted successfully!');
      fetchDivisions();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Division deletion error:', error);
    }
  };

  const getYearLabel = (year: number) => {
    switch (year) {
      case 1: return 'FY (First Year)';
      case 2: return 'SY (Second Year)';
      case 3: return 'TY (Third Year)';
      case 4: return 'BTech (Final Year)';
      default: return `Year ${year}`;
    }
  };

  const getYearColor = (year: number) => {
    switch (year) {
      case 1: return 'bg-green-100 text-green-800';
      case 2: return 'bg-blue-100 text-blue-800';
      case 3: return 'bg-purple-100 text-purple-800';
      case 4: return 'bg-orange-100 text-orange-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  // Filter divisions based on filters
  const filteredDivisions = divisions.filter((division) => {
    const matchesYear = filterYear === '' || 
      division.year.toString() === filterYear;
    
    const matchesDepartment = filterDepartment === '' || 
      division.department.id.toString() === filterDepartment;
    
    const matchesAcademicYear = filterAcademicYear === '' || 
      division.academicYear.id.toString() === filterAcademicYear;
    
    return matchesYear && matchesDepartment && matchesAcademicYear;
  });

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Divisions</h1>
          <p className="text-gray-600 mt-1">Manage student divisions and sections</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Division
        </Button>
      </div>

      {/* Filters */}
      <Card className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Filter by Year
            </label>
            <select
              value={filterYear}
              onChange={(e) => setFilterYear(e.target.value)}
              className="input-field"
            >
              <option value="">All Years</option>
              <option value="1">FY (First Year)</option>
              <option value="2">SY (Second Year)</option>
              <option value="3">TY (Third Year)</option>
              <option value="4">BTech (Final Year)</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Filter by Department
            </label>
            <select
              value={filterDepartment}
              onChange={(e) => setFilterDepartment(e.target.value)}
              className="input-field"
            >
              <option value="">All Departments</option>
              {departments.map((dept) => (
                <option key={dept.id} value={dept.id}>
                  {dept.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Filter by Academic Year
            </label>
            <select
              value={filterAcademicYear}
              onChange={(e) => setFilterAcademicYear(e.target.value)}
              className="input-field"
            >
              <option value="">All Academic Years</option>
              {academicYears.map((year) => (
                <option key={year.id} value={year.id}>
                  {year.yearName} {year.isCurrent ? '(Current)' : ''}
                </option>
              ))}
            </select>
          </div>
        </div>
        {(filterYear || filterDepartment || filterAcademicYear) && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <span className="text-gray-600">
              Showing {filteredDivisions.length} of {divisions.length} divisions
            </span>
            <button
              onClick={() => {
                setFilterYear('');
                setFilterDepartment('');
                setFilterAcademicYear('');
              }}
              className="text-primary-600 hover:text-primary-700 font-medium"
            >
              Clear Filters
            </button>
          </div>
        )}
      </Card>

      {/* Divisions Grid */}
      {filteredDivisions.length === 0 ? (
        <Card>
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">
              {divisions.length === 0 ? 'No divisions found. Create your first division to get started.' : 'No divisions match your filters.'}
            </p>
            {divisions.length > 0 && (
              <button
                onClick={() => {
                  setFilterYear('');
                  setFilterDepartment('');
                  setFilterAcademicYear('');
                }}
                className="mt-4 text-primary-600 hover:text-primary-700 font-medium"
              >
                Clear Filters
              </button>
            )}
          </div>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredDivisions.map((division) => (
          <Card key={division.id} hover>
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-start gap-3">
                <div className="p-3 bg-primary-100 text-primary-600 rounded-lg">
                  <FiUsers size={24} />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-gray-900">
                    {getYearLabel(division.year).split(' ')[0]}-{division.name}
                  </h3>
                  <p className="text-sm text-gray-600">{division.branch}</p>
                </div>
              </div>
              <div className="flex gap-2">
                <button 
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                  onClick={() => handleEdit(division)}
                  title="Edit"
                >
                  <FiEdit2 size={18} />
                </button>
                <button 
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                  onClick={() => handleDelete(division.id, division.name, division.year)}
                >
                  <FiTrash2 size={18} />
                </button>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Year:</span>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getYearColor(division.year)}`}>
                  {getYearLabel(division.year)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Students:</span>
                <span className="font-medium">{division.totalStudents}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Department:</span>
                <span className="font-medium text-xs">{division.department.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Academic Year:</span>
                <span className="font-medium text-xs">{division.academicYear.yearName}</span>
              </div>
            </div>
          </Card>
          ))}
        </div>
      )}

      {/* Add/Edit Division Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Division" : "Add New Division"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Division Name"
              value={formData.name}
              onChange={(e) => {
                setFormData({ ...formData, name: e.target.value.toUpperCase() });
                setErrors({ ...errors, name: '' });
              }}
              error={errors.name}
              placeholder="A"
              maxLength={2}
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Year
              </label>
              <select
                value={formData.year}
                onChange={(e) => setFormData({ ...formData, year: e.target.value })}
                className="input-field"
              >
                <option value="1">FY (First Year)</option>
                <option value="2">SY (Second Year)</option>
                <option value="3">TY (Third Year)</option>
                <option value="4">BTech (Final Year)</option>
              </select>
            </div>
          </div>

          <Input
            label="Branch"
            value={formData.branch}
            onChange={(e) => {
              setFormData({ ...formData, branch: e.target.value });
              setErrors({ ...errors, branch: '' });
            }}
            error={errors.branch}
            placeholder="Computer Science"
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Department
            </label>
            <select
              value={formData.departmentId}
              onChange={(e) => {
                setFormData({ ...formData, departmentId: e.target.value });
                setErrors({ ...errors, departmentId: '' });
              }}
              className={`input-field ${errors.departmentId ? 'input-error' : ''}`}
            >
              <option value="">Select Department</option>
              {departments.map((dept) => (
                <option key={dept.id} value={dept.id}>
                  {dept.name}
                </option>
              ))}
            </select>
            {errors.departmentId && (
              <p className="mt-1 text-sm text-red-600">{errors.departmentId}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Academic Year
            </label>
            <select
              value={formData.academicYearId}
              onChange={(e) => {
                setFormData({ ...formData, academicYearId: e.target.value });
                setErrors({ ...errors, academicYearId: '' });
              }}
              className={`input-field ${errors.academicYearId ? 'input-error' : ''}`}
            >
              <option value="">Select Academic Year</option>
              {academicYears.map((year) => (
                <option key={year.id} value={year.id}>
                  {year.yearName} {year.isCurrent ? '(Current)' : ''}
                </option>
              ))}
            </select>
            {errors.academicYearId && (
              <p className="mt-1 text-sm text-red-600">{errors.academicYearId}</p>
            )}
          </div>

          <Input
            label="Total Students"
            type="number"
            value={formData.totalStudents}
            onChange={(e) => {
              setFormData({ ...formData, totalStudents: e.target.value });
              setErrors({ ...errors, totalStudents: '' });
            }}
            error={errors.totalStudents}
            placeholder="60"
            min="1"
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
              {isEditMode ? 'Update Division' : 'Create Division'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
