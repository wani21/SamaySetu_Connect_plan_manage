import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit2, FiTrash2, FiBook, FiCheck, FiX } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { courseAPI, departmentAPI } from '../../services/api';
import { Modal } from '../common/Modal';
import { getErrorMessage } from '../../utils/errorHandler';

export const CoursesPage: React.FC = () => {
  const [courses, setCourses] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingCourse, setEditingCourse] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    shortName: '',
    courseType: 'theory',
    credits: '',
    hoursPerWeek: '',
    departmentId: '',
    semester: '1',
    year: '1',
    description: '',
  });
  const [errors, setErrors] = useState<any>({});
  const [shortNameAvailable, setShortNameAvailable] = useState<boolean | null>(null);
  const [shortNameSuggestions, setShortNameSuggestions] = useState<string[]>([]);
  const [checkingShortName, setCheckingShortName] = useState(false);
  
  // Filter states
  const [searchQuery, setSearchQuery] = useState('');
  const [filterDepartment, setFilterDepartment] = useState('');
  const [filterSemester, setFilterSemester] = useState('');

  useEffect(() => {
    fetchCourses();
    fetchDepartments();
  }, []);

  const fetchCourses = async () => {
    try {
      const response = await courseAPI.getAll();
      setCourses(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      toast.error('Failed to fetch courses');
      console.error(error);
      setCourses([]);
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

  // Short name validation with debounce
  const checkShortNameAvailability = async (shortName: string) => {
    if (!shortName || shortName.length < 2 || !formData.departmentId || !formData.year) {
      setShortNameAvailable(null);
      setShortNameSuggestions([]);
      return;
    }

    // Validate format (2-15 alphanumeric with spaces and hyphens, case-insensitive)
    if (!/^[A-Za-z0-9 -]{2,15}$/.test(shortName)) {
      setShortNameAvailable(false);
      setErrors((prev: any) => ({ ...prev, shortName: 'Must be 2-15 letters, numbers, spaces, and hyphens only' }));
      setShortNameSuggestions([]);
      return;
    }

    setCheckingShortName(true);
    try {
      const response = await courseAPI.checkShortName(
        shortName,
        parseInt(formData.departmentId),
        parseInt(formData.year),
        editingCourse?.id
      );
      
      const data = response.data;
      setShortNameAvailable(data.available);
      
      if (!data.available) {
        setErrors((prev: any) => ({ ...prev, shortName: data.message }));
        setShortNameSuggestions(data.suggestions || []);
      } else {
        setErrors((prev: any) => {
          const newErrors = { ...prev };
          delete newErrors.shortName;
          return newErrors;
        });
        setShortNameSuggestions([]);
      }
    } catch (error) {
      console.error('Error checking short name:', error);
    } finally {
      setCheckingShortName(false);
    }
  };

  // Debounced short name check
  React.useEffect(() => {
    const timer = setTimeout(() => {
      if (formData.shortName && formData.departmentId && formData.year) {
        checkShortNameAvailability(formData.shortName);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [formData.shortName, formData.departmentId, formData.year]);

  const handleEdit = (course: any) => {
    setEditingCourse(course);
    setIsEditMode(true);
    const semesterNum = course.semester.replace('SEM_', '');
    setFormData({
      name: course.name,
      code: course.code,
      shortName: course.shortName || '',
      courseType: course.courseType.toLowerCase(),
      credits: course.credits.toString(),
      hoursPerWeek: course.hoursPerWeek.toString(),
      departmentId: course.department.id.toString(),
      semester: semesterNum,
      year: course.year?.toString() || '1',
      description: course.description || '',
    });
    setShortNameAvailable(true); // Existing short name is valid
    setShortNameSuggestions([]);
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      name: '',
      code: '',
      shortName: '',
      courseType: 'theory',
      credits: '',
      hoursPerWeek: '',
      departmentId: '',
      semester: '1',
      year: '1',
      description: '',
    });
    setEditingCourse(null);
    setIsEditMode(false);
    setErrors({});
    setShortNameAvailable(null);
    setShortNameSuggestions([]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.name) newErrors.name = 'Name is required';
    if (!formData.code) newErrors.code = 'Code is required';
    if (!formData.shortName) newErrors.shortName = 'Short name is required';
    else if (!/^[A-Za-z0-9 -]{2,15}$/.test(formData.shortName)) newErrors.shortName = 'Must be 2-15 letters, numbers, spaces, and hyphens only';
    else if (shortNameAvailable === false) newErrors.shortName = 'Short name is already taken';
    if (!formData.credits) newErrors.credits = 'Credits is required';
    if (!formData.hoursPerWeek) newErrors.hoursPerWeek = 'Hours per week is required';
    if (!formData.departmentId) newErrors.departmentId = 'Department is required';
    if (!formData.year) newErrors.year = 'Year is required';
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      const courseData = {
        name: formData.name,
        code: formData.code,
        shortName: formData.shortName,
        courseType: formData.courseType.toUpperCase(),
        credits: parseInt(formData.credits),
        hoursPerWeek: parseInt(formData.hoursPerWeek),
        semester: `SEM_${formData.semester}`,
        year: parseInt(formData.year),
        description: formData.description || null,
        department: {
          id: parseInt(formData.departmentId)
        },
      };

      if (isEditMode && editingCourse) {
        await courseAPI.update(editingCourse.id, courseData);
        toast.success('Course updated successfully!');
      } else {
        await courseAPI.create(courseData);
        toast.success('Course created successfully!');
      }

      setShowModal(false);
      resetForm();
      fetchCourses();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Course operation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete ${name}?`)) {
      return;
    }

    try {
      await courseAPI.delete(id);
      toast.success('Course deleted successfully!');
      fetchCourses();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Course deletion error:', error);
    }
  };

  // Filter courses based on search and filters
  const filteredCourses = courses.filter((course) => {
    const matchesSearch = searchQuery === '' || 
      course.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      course.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (course.shortName && course.shortName.toLowerCase().includes(searchQuery.toLowerCase()));
    
    const matchesDepartment = filterDepartment === '' || 
      course.department.id.toString() === filterDepartment;
    
    const matchesSemester = filterSemester === '' || 
      course.semester === `SEM_${filterSemester}`;
    
    return matchesSearch && matchesDepartment && matchesSemester;
  });

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Courses</h1>
          <p className="text-gray-600 mt-1">Manage courses and subjects</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Course
        </Button>
      </div>

      {/* Filters */}
      <Card className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <Input
              label="Search"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by name, short name, or code..."
            />
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
              Filter by Semester
            </label>
            <select
              value={filterSemester}
              onChange={(e) => setFilterSemester(e.target.value)}
              className="input-field"
            >
              <option value="">All Semesters</option>
              {[1, 2, 3, 4, 5, 6, 7, 8].map((sem) => (
                <option key={sem} value={sem}>
                  Semester {sem}
                </option>
              ))}
            </select>
          </div>
        </div>
        {(searchQuery || filterDepartment || filterSemester) && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <span className="text-gray-600">
              Showing {filteredCourses.length} of {courses.length} courses
            </span>
            <button
              onClick={() => {
                setSearchQuery('');
                setFilterDepartment('');
                setFilterSemester('');
              }}
              className="text-primary-600 hover:text-primary-700 font-medium"
            >
              Clear Filters
            </button>
          </div>
        )}
      </Card>

      {/* Courses Grid */}
      {filteredCourses.length === 0 ? (
        <Card>
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">
              {courses.length === 0 ? 'No courses found. Create your first course to get started.' : 'No courses match your filters.'}
            </p>
            {courses.length > 0 && (
              <button
                onClick={() => {
                  setSearchQuery('');
                  setFilterDepartment('');
                  setFilterSemester('');
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
          {filteredCourses.map((course) => (
          <Card key={course.id} hover>
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-start gap-3">
                <div className={`p-2 rounded-lg ${course.courseType === 'LAB' ? 'bg-purple-100 text-purple-600' : 'bg-blue-100 text-blue-600'}`}>
                  <FiBook size={20} />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-gray-900">
                    {course.name} {course.shortName && <span className="text-primary-600">({course.shortName})</span>}
                  </h3>
                  <p className="text-sm text-gray-600">{course.code}</p>
                </div>
              </div>
              <div className="flex gap-2">
                <button 
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                  onClick={() => handleEdit(course)}
                  title="Edit"
                >
                  <FiEdit2 size={18} />
                </button>
                <button 
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                  onClick={() => handleDelete(course.id, course.name)}
                >
                  <FiTrash2 size={18} />
                </button>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">Type:</span>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                  course.courseType === 'LAB' ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800'
                }`}>
                  {course.courseType}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Credits:</span>
                <span className="font-medium">{course.credits}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Hours/Week:</span>
                <span className="font-medium">{course.hoursPerWeek}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Semester:</span>
                <span className="font-medium">{course.semester}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Department:</span>
                <span className="font-medium text-xs">{course.department.name}</span>
              </div>
            </div>
          </Card>
          ))}
        </div>
      )}

      {/* Add/Edit Course Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Course" : "Add New Course"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Course Name"
              value={formData.name}
              onChange={(e) => {
                setFormData({ ...formData, name: e.target.value });
                setErrors({ ...errors, name: '' });
              }}
              error={errors.name}
              placeholder="Data Structures"
            />

            <Input
              label="Course Code"
              value={formData.code}
              onChange={(e) => {
                setFormData({ ...formData, code: e.target.value });
                setErrors({ ...errors, code: '' });
              }}
              error={errors.code}
              placeholder="CS301"
            />
          </div>

          <div>
            <Input
              label="Short Name"
              value={formData.shortName}
              onChange={(e) => {
                const value = e.target.value.replace(/[^A-Za-z0-9 -]/g, '');
                if (value.length <= 15) {
                  setFormData({ ...formData, shortName: value });
                }
              }}
              error={errors.shortName}
              placeholder="SE-Lab"
              maxLength={15}
            />
            {checkingShortName && (
              <p className="mt-1 text-sm text-gray-500">Checking availability...</p>
            )}
            {shortNameAvailable === true && formData.shortName && (
              <p className="mt-1 text-sm text-green-600 flex items-center gap-1">
                <FiCheck size={14} /> Available
              </p>
            )}
            {shortNameAvailable === false && formData.shortName && (
              <div className="mt-1">
                <p className="text-sm text-red-600 flex items-center gap-1">
                  <FiX size={14} /> {errors.shortName}
                </p>
                {shortNameSuggestions.length > 0 && (
                  <div className="mt-2">
                    <p className="text-xs text-gray-600 mb-1">Suggestions:</p>
                    <div className="flex flex-wrap gap-2">
                      {shortNameSuggestions.map((suggestion) => (
                        <button
                          key={suggestion}
                          type="button"
                          onClick={() => {
                            setFormData({ ...formData, shortName: suggestion });
                          }}
                          className="px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded hover:bg-blue-100"
                        >
                          {suggestion}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}
            <p className="mt-1 text-xs text-gray-500">2-15 letters, numbers, spaces, and hyphens (A-Z, a-z, 0-9, space, -)</p>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Course Type
              </label>
              <select
                value={formData.courseType}
                onChange={(e) => setFormData({ ...formData, courseType: e.target.value })}
                className="input-field"
              >
                <option value="theory">Theory</option>
                <option value="lab">Lab</option>
              </select>
            </div>

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
                Year
              </label>
              <select
                value={formData.year}
                onChange={(e) => {
                  setFormData({ ...formData, year: e.target.value });
                  setErrors({ ...errors, year: '' });
                }}
                className={`input-field ${errors.year ? 'input-error' : ''}`}
              >
                <option value="">Select Year</option>
                <option value="1">First Year (FY)</option>
                <option value="2">Second Year (SY)</option>
                <option value="3">Third Year (TY)</option>
                <option value="4">Fourth Year (B.Tech)</option>
              </select>
              {errors.year && (
                <p className="mt-1 text-sm text-red-600">{errors.year}</p>
              )}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <Input
              label="Credits"
              type="number"
              value={formData.credits}
              onChange={(e) => {
                setFormData({ ...formData, credits: e.target.value });
                setErrors({ ...errors, credits: '' });
              }}
              error={errors.credits}
              placeholder="4"
              min="1"
            />

            <Input
              label="Hours/Week"
              type="number"
              value={formData.hoursPerWeek}
              onChange={(e) => {
                setFormData({ ...formData, hoursPerWeek: e.target.value });
                setErrors({ ...errors, hoursPerWeek: '' });
              }}
              error={errors.hoursPerWeek}
              placeholder="4"
              min="1"
            />

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Semester
              </label>
              <select
                value={formData.semester}
                onChange={(e) => setFormData({ ...formData, semester: e.target.value })}
                className="input-field"
              >
                {[1, 2, 3, 4, 5, 6, 7, 8].map((sem) => (
                  <option key={sem} value={sem}>
                    {sem}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description (Optional)
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="input-field"
              rows={3}
              placeholder="Course description..."
            />
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
              {isEditMode ? 'Update Course' : 'Create Course'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
