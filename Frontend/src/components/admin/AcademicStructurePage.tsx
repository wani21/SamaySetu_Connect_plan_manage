import React, { useState, useEffect, useRef } from 'react';
import { FiChevronRight, FiPlus, FiEdit2, FiTrash2, FiCalendar, FiLayers, FiUsers, FiBook, FiGrid, FiUpload, FiDownload, FiCopy } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { Modal } from '../common/Modal';
import { academicYearAPI, departmentAPI, divisionAPI, courseAPI, batchAPI, adminAPI, teacherAdminAPI } from '../../services/api';
import { getErrorMessage } from '../../utils/errorHandler';

type ViewLevel = 'academic-years' | 'year-overview' | 'department-detail' | 'batches';

interface BreadcrumbItem {
  label: string;
  level: ViewLevel;
  data?: any;
}

// Map frontend semester to backend enum
const semesterToBackend = (sem: 'ODD' | 'EVEN', year: number): string => {
  const semNum = (year - 1) * 2 + (sem === 'ODD' ? 1 : 2);
  return `SEM_${semNum}`;
};

export const AcademicStructurePage: React.FC = () => {
  const [currentLevel, setCurrentLevel] = useState<ViewLevel>('academic-years');
  const [breadcrumbs, setBreadcrumbs] = useState<BreadcrumbItem[]>([{ label: 'Academic Years', level: 'academic-years' }]);
  const [selectedAcademicYear, setSelectedAcademicYear] = useState<any>(null);
  const [selectedYear, setSelectedYear] = useState<number | null>(null);
  const [selectedDepartment, setSelectedDepartment] = useState<any>(null);
  const [selectedDivision, setSelectedDivision] = useState<any>(null);
  const [selectedSemester, setSelectedSemester] = useState<'ODD' | 'EVEN'>('ODD');

  const [academicYears, setAcademicYears] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [divisions, setDivisions] = useState<any[]>([]);
  const [courses, setCourses] = useState<any[]>([]);
  const [batches, setBatches] = useState<any[]>([]);
  const [staffList, setStaffList] = useState<any[]>([]);
  
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [modalType, setModalType] = useState<string>('');
  const [editingItem, setEditingItem] = useState<any>(null);
  const [formData, setFormData] = useState<any>({});

  const [showUploadModal, setShowUploadModal] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Copy departments modal states
  const [showCopyModal, setShowCopyModal] = useState(false);
  const [sourceAcademicYear, setSourceAcademicYear] = useState<any>(null);
  const [sourceDepartments, setSourceDepartments] = useState<any[]>([]);
  const [selectedDeptsToCopy, setSelectedDeptsToCopy] = useState<number[]>([]);

  // Autocomplete states
  const [hodSuggestions, setHodSuggestions] = useState<any[]>([]);
  const [showHodSuggestions, setShowHodSuggestions] = useState(false);
  const [teacherSuggestions, setTeacherSuggestions] = useState<any[]>([]);
  const [showTeacherSuggestions, setShowTeacherSuggestions] = useState(false);

  const yearLabels: { [key: number]: { short: string; full: string } } = {
    1: { short: 'FY', full: 'First Year' },
    2: { short: 'SY', full: 'Second Year' },
    3: { short: 'TY', full: 'Third Year' },
    4: { short: 'B.Tech', full: 'Fourth Year' },
  };

  const scheduleLabels: { [key: string]: string } = {
    'TYPE_1': 'Schedule 1',
    'TYPE_2': 'Schedule 2',
  };

  useEffect(() => {
    fetchAcademicYears();
    fetchStaffList();
  }, []);

  const fetchAcademicYears = async () => {
    try {
      const response = await academicYearAPI.getAll();
      setAcademicYears(Array.isArray(response.data) ? response.data : []);
    } catch (error) { toast.error('Failed to fetch academic years'); }
  };

  const fetchDepartments = async (academicYearId?: number) => {
    try {
      if (academicYearId) {
        const response = await departmentAPI.getByAcademicYear(academicYearId);
        setDepartments(Array.isArray(response.data) ? response.data : []);
      } else {
        setDepartments([]);
      }
    } catch (error) { toast.error('Failed to fetch departments'); }
  };

  const fetchDivisions = async () => {
    try {
      const response = await divisionAPI.getAll();
      setDivisions(Array.isArray(response.data) ? response.data : []);
    } catch (error) { toast.error('Failed to fetch divisions'); }
  };

  const fetchCourses = async () => {
    try {
      const response = await courseAPI.getAll();
      setCourses(Array.isArray(response.data) ? response.data : []);
    } catch (error) { toast.error('Failed to fetch courses'); }
  };

  const fetchBatches = async () => {
    try {
      const response = await batchAPI.getAll();
      setBatches(Array.isArray(response.data) ? response.data : []);
    } catch (error) { toast.error('Failed to fetch batches'); }
  };

  const fetchStaffList = async () => {
    try {
      const response = await teacherAdminAPI.getAll();
      const activeStaff = (Array.isArray(response.data) ? response.data : [])
        .filter((s: any) => s.isActive && s.isEmailVerified);
      setStaffList(activeStaff);
    } catch (error) { console.error('Failed to fetch faculty'); }
  };

  const filterStaffSuggestions = (query: string) => {
    if (!query || query.length < 2) return [];
    const lowerQuery = query.toLowerCase();
    return staffList.filter(s => s.name?.toLowerCase().includes(lowerQuery)).slice(0, 5);
  };


  const navigateTo = (level: ViewLevel, item?: any) => {
    switch (level) {
      case 'year-overview':
        setSelectedAcademicYear(item);
        fetchDepartments(item.id);
        fetchDivisions();
        fetchCourses();
        setBreadcrumbs([
          { label: 'Academic Years', level: 'academic-years' },
          { label: item.yearName, level: 'year-overview', data: item }
        ]);
        break;
      case 'department-detail':
        setSelectedYear(item.year);
        setSelectedDepartment(item.dept);
        setSelectedSemester('ODD');
        fetchBatches();
        setBreadcrumbs([
          { label: 'Academic Years', level: 'academic-years' },
          { label: selectedAcademicYear.yearName, level: 'year-overview', data: selectedAcademicYear },
          { label: `${yearLabels[item.year].short} - ${item.dept.name}`, level: 'department-detail', data: item }
        ]);
        break;
      case 'batches':
        setSelectedDivision(item);
        setBreadcrumbs([
          { label: 'Academic Years', level: 'academic-years' },
          { label: selectedAcademicYear.yearName, level: 'year-overview', data: selectedAcademicYear },
          { label: `${yearLabels[selectedYear!].short} - ${selectedDepartment.name}`, level: 'department-detail', data: { year: selectedYear, dept: selectedDepartment } },
          { label: `Division ${item.name}`, level: 'batches', data: item }
        ]);
        break;
    }
    setCurrentLevel(level);
  };

  const navigateBack = (targetLevel: ViewLevel, data?: any) => {
    setCurrentLevel(targetLevel);
    switch (targetLevel) {
      case 'academic-years':
        setSelectedAcademicYear(null);
        setSelectedYear(null);
        setSelectedDepartment(null);
        setSelectedDivision(null);
        setBreadcrumbs([{ label: 'Academic Years', level: 'academic-years' }]);
        break;
      case 'year-overview':
        setSelectedYear(null);
        setSelectedDepartment(null);
        setSelectedDivision(null);
        setBreadcrumbs([
          { label: 'Academic Years', level: 'academic-years' },
          { label: data.yearName, level: 'year-overview', data }
        ]);
        break;
      case 'department-detail':
        setSelectedDivision(null);
        break;
    }
  };

  const openAddModal = (type: string) => {
    setModalType(type);
    setEditingItem(null);
    setFormData(getDefaultFormData(type));
    setShowModal(true);
    setShowHodSuggestions(false);
    setShowTeacherSuggestions(false);
  };

  const openEditModal = (type: string, item: any) => {
    setModalType(type);
    setEditingItem(item);
    setFormData(getEditFormData(type, item));
    setShowModal(true);
    setShowHodSuggestions(false);
    setShowTeacherSuggestions(false);
  };

  const getDefaultFormData = (type: string) => {
    switch (type) {
      case 'academic-year': return { yearName: '', startDate: '', endDate: '', isCurrent: false };
      case 'department': return { name: '', code: '', headOfDepartment: '', years: [1, 2, 3, 4] };
      case 'division': return { name: '', branch: '', totalStudents: '', timeSlotType: 'TYPE_1', classTeacher: '', classRepresentative: '' };
      case 'course': return { name: '', code: '', courseType: 'THEORY', credits: '', hoursPerWeek: '' };
      case 'batch': return { name: '' };
      default: return {};
    }
  };

  const getEditFormData = (type: string, item: any) => {
    switch (type) {
      case 'academic-year': return { yearName: item.yearName, startDate: item.startDate, endDate: item.endDate, isCurrent: item.isCurrent };
      case 'department': 
        const yearsArray = item.years ? item.years.split(',').map((y: string) => parseInt(y.trim())) : [1, 2, 3, 4];
        return { name: item.name, code: item.code, headOfDepartment: item.headOfDepartment || '', years: yearsArray };
      case 'division': return { name: item.name, branch: item.branch, totalStudents: item.totalStudents?.toString(), timeSlotType: item.timeSlotType || 'TYPE_1', classTeacher: item.classTeacher || '', classRepresentative: item.classRepresentative || '' };
      case 'course': return { name: item.name, code: item.code, courseType: item.courseType, credits: item.credits?.toString(), hoursPerWeek: item.hoursPerWeek?.toString() };
      case 'batch': return { name: item.name };
      default: return {};
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      switch (modalType) {
        case 'academic-year':
          if (editingItem) { await academicYearAPI.update(editingItem.id, formData); toast.success('Academic year updated!'); }
          else { await academicYearAPI.create(formData); toast.success('Academic year created!'); }
          fetchAcademicYears();
          break;
        case 'department':
          const deptData = { 
            ...formData, 
            years: formData.years.join(','),
            academicYear: selectedAcademicYear ? { id: selectedAcademicYear.id } : null
          };
          if (editingItem) { await departmentAPI.update(editingItem.id, deptData); toast.success('Department updated!'); }
          else { await departmentAPI.create(deptData); toast.success('Department created!'); }
          fetchDepartments(selectedAcademicYear?.id);
          break;
        case 'division':
          const divisionData = { 
            ...formData, 
            year: selectedYear, 
            totalStudents: parseInt(formData.totalStudents) || 0, 
            department: { id: selectedDepartment.id }, 
            academicYear: { id: selectedAcademicYear.id } 
          };
          if (editingItem) { await divisionAPI.update(editingItem.id, divisionData); toast.success('Division updated!'); }
          else { await divisionAPI.create(divisionData); toast.success('Division created!'); }
          fetchDivisions();
          break;
        case 'course':
          const backendSemester = semesterToBackend(selectedSemester, selectedYear!);
          const courseData = { 
            ...formData, 
            credits: parseInt(formData.credits), 
            hoursPerWeek: parseInt(formData.hoursPerWeek), 
            department: { id: selectedDepartment.id }, 
            year: selectedYear,
            semester: backendSemester
          };
          if (editingItem) { await courseAPI.update(editingItem.id, courseData); toast.success('Course updated!'); }
          else { await courseAPI.create(courseData); toast.success('Course created!'); }
          fetchCourses();
          break;
        case 'batch':
          const batchData = { ...formData, division: { id: selectedDivision.id } };
          if (editingItem) { await batchAPI.update(editingItem.id, batchData); toast.success('Batch updated!'); }
          else { await batchAPI.create(batchData); toast.success('Batch created!'); }
          fetchBatches();
          break;
      }
      setShowModal(false);
    } catch (error: any) { 
      toast.error(getErrorMessage(error)); 
    } finally { 
      setIsLoading(false); 
    }
  };

  const handleDelete = async (type: string, id: number, name: string) => {
    if (!window.confirm(`Delete "${name}"?`)) return;
    try {
      switch (type) {
        case 'academic-year': await academicYearAPI.delete(id); fetchAcademicYears(); break;
        case 'department': await departmentAPI.delete(id); fetchDepartments(selectedAcademicYear?.id); break;
        case 'division': await divisionAPI.delete(id); fetchDivisions(); break;
        case 'course': await courseAPI.delete(id); fetchCourses(); break;
        case 'batch': await batchAPI.delete(id); fetchBatches(); break;
      }
      toast.success('Deleted!');
    } catch (error: any) { toast.error(getErrorMessage(error)); }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (!file.name.toLowerCase().endsWith('.csv')) {
        toast.error('Please select a CSV file');
        return;
      }
      setSelectedFile(file);
    }
  };

  const handleCourseUpload = async () => {
    if (!selectedFile || !selectedDepartment || !selectedYear) return;
    setIsLoading(true);
    try {
      const response = await adminAPI.uploadCourses(selectedFile, selectedDepartment.id, selectedYear);
      toast.success(response.data);
      setSelectedFile(null);
      setShowUploadModal(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
      fetchCourses();
    } catch (error: any) {
      toast.error(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      const response = await adminAPI.downloadCoursesTemplate();
      const blob = new Blob([response.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'courses_template.csv';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success('Template downloaded');
    } catch (error) {
      toast.error('Failed to download template');
    }
  };

  const openCopyModal = async () => {
    // Find previous academic years to copy from
    const sortedYears = [...academicYears].sort((a, b) => b.yearName.localeCompare(a.yearName));
    const currentIndex = sortedYears.findIndex(y => y.id === selectedAcademicYear?.id);
    
    // Get the previous year (or current if this is the first)
    const previousYear = sortedYears[currentIndex + 1] || sortedYears[currentIndex - 1];
    
    if (!previousYear) {
      toast.error('No other academic years available to copy from');
      return;
    }
    
    setSourceAcademicYear(previousYear);
    
    // Fetch departments from source year
    try {
      const response = await departmentAPI.getByAcademicYear(previousYear.id);
      setSourceDepartments(Array.isArray(response.data) ? response.data : []);
      setSelectedDeptsToCopy([]);
      setShowCopyModal(true);
    } catch (error) {
      toast.error('Failed to fetch departments from source year');
    }
  };

  const handleSourceYearChange = async (yearId: number) => {
    const year = academicYears.find(y => y.id === yearId);
    setSourceAcademicYear(year);
    
    try {
      const response = await departmentAPI.getByAcademicYear(yearId);
      setSourceDepartments(Array.isArray(response.data) ? response.data : []);
      setSelectedDeptsToCopy([]);
    } catch (error) {
      toast.error('Failed to fetch departments');
    }
  };

  const handleCopyDepartments = async () => {
    if (selectedDeptsToCopy.length === 0) {
      toast.error('Please select at least one department to copy');
      return;
    }
    
    setIsLoading(true);
    try {
      await departmentAPI.copyToAcademicYear(
        sourceAcademicYear.id,
        selectedAcademicYear.id,
        selectedDeptsToCopy
      );
      toast.success(`${selectedDeptsToCopy.length} department(s) copied successfully!`);
      setShowCopyModal(false);
      fetchDepartments(selectedAcademicYear.id);
    } catch (error: any) {
      toast.error(getErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  };

  // Filter functions - now academic year specific
  const getDepartmentsForYear = (year: number) => departments.filter(d => {
    const deptYears = d.years ? d.years.split(',').map((y: string) => parseInt(y.trim())) : [1, 2, 3, 4];
    return deptYears.includes(year);
  });

  const getFilteredDivisions = () => divisions.filter(d => 
    d.academicYear?.id === selectedAcademicYear?.id && 
    d.year === selectedYear && 
    d.department?.id === selectedDepartment?.id
  );

  const getFilteredCourses = () => {
    const backendSem = semesterToBackend(selectedSemester, selectedYear!);
    return courses.filter(c => 
      c.department?.id === selectedDepartment?.id && 
      c.year === selectedYear && 
      c.semester === backendSem
    );
  };

  const getFilteredBatches = () => batches.filter(b => b.division?.id === selectedDivision?.id);


  const renderBreadcrumbs = () => (
    <nav className="flex items-center gap-2 text-sm bg-gray-50 px-4 py-2 rounded-lg mb-6">
      {breadcrumbs.map((crumb, index) => (
        <React.Fragment key={index}>
          {index > 0 && <FiChevronRight className="text-gray-400" />}
          <button onClick={() => navigateBack(crumb.level, crumb.data)} className={`px-2 py-1 rounded ${index === breadcrumbs.length - 1 ? 'bg-primary-100 text-primary-700 font-medium' : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'}`}>
            {crumb.label}
          </button>
        </React.Fragment>
      ))}
    </nav>
  );

  const renderAcademicYears = () => (
    <>
      <div className="flex justify-between items-center mb-6">
        <p className="text-gray-600">Select an academic year to manage its structure</p>
        <Button variant="primary" onClick={() => openAddModal('academic-year')} className="flex items-center gap-2">
          <FiPlus /> Add Academic Year
        </Button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {academicYears.map((year) => (
          <Card key={year.id} hover onClick={() => navigateTo('year-overview', year)}>
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="p-3 bg-primary-100 text-primary-600 rounded-lg"><FiCalendar size={24} /></div>
                <div>
                  <h3 className="text-lg font-bold text-gray-900">{year.yearName}</h3>
                  <p className="text-sm text-gray-500">{year.startDate} to {year.endDate}</p>
                  {year.isCurrent && <span className="inline-block mt-1 px-2 py-0.5 bg-green-100 text-green-800 rounded text-xs">Current</span>}
                </div>
              </div>
              <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                <button onClick={() => openEditModal('academic-year', year)} className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"><FiEdit2 size={16} /></button>
                <button onClick={() => handleDelete('academic-year', year.id, year.yearName)} className="p-2 text-red-600 hover:bg-red-50 rounded-lg"><FiTrash2 size={16} /></button>
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm text-primary-600"><span>View Structure</span><FiChevronRight className="ml-1" /></div>
          </Card>
        ))}
      </div>
    </>
  );

  const renderYearOverview = () => (
    <>
      <div className="flex justify-between items-center mb-6">
        <p className="text-gray-600">Select a year and department to manage divisions and courses</p>
        <div className="flex gap-2">
          {academicYears.length > 1 && (
            <Button variant="outline" onClick={openCopyModal} className="flex items-center gap-2">
              <FiCopy /> Copy from Other Year
            </Button>
          )}
          <Button variant="primary" onClick={() => openAddModal('department')} className="flex items-center gap-2">
            <FiPlus /> Add Department
          </Button>
        </div>
      </div>

      {/* Year Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        {[1, 2, 3, 4].map((year) => {
          const yearDepts = getDepartmentsForYear(year);
          return (
            <div key={year} className={`p-4 rounded-xl border-2 ${year === 1 ? 'border-green-200 bg-green-50' : year === 2 ? 'border-blue-200 bg-blue-50' : year === 3 ? 'border-purple-200 bg-purple-50' : 'border-orange-200 bg-orange-50'}`}>
              <div className="text-center mb-3">
                <div className={`mx-auto w-12 h-12 rounded-full flex items-center justify-center mb-2 ${year === 1 ? 'bg-green-100 text-green-600' : year === 2 ? 'bg-blue-100 text-blue-600' : year === 3 ? 'bg-purple-100 text-purple-600' : 'bg-orange-100 text-orange-600'}`}>
                  <FiLayers size={20} />
                </div>
                <h3 className="font-bold text-gray-900">{yearLabels[year].short}</h3>
                <p className="text-xs text-gray-500">{yearLabels[year].full}</p>
              </div>
              <div className="space-y-1 max-h-32 overflow-y-auto">
                {yearDepts.length === 0 ? (
                  <p className="text-xs text-gray-400 text-center">No departments</p>
                ) : (
                  yearDepts.map((dept) => (
                    <button key={dept.id} onClick={() => navigateTo('department-detail', { year, dept })} className="w-full text-left px-2 py-1.5 text-sm bg-white rounded hover:bg-gray-50 flex items-center justify-between group">
                      <span className="truncate">{dept.code}</span>
                      <FiChevronRight className="text-gray-400 group-hover:text-gray-600" size={14} />
                    </button>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* All Departments Table */}
      <div className="border-t pt-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold text-gray-900">All Departments</h3>
          <span className="text-sm text-gray-500">{selectedAcademicYear?.yearName}</span>
        </div>
        <Card>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-4 font-medium text-gray-700">Name</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">Code</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">HOD</th>
                  <th className="text-left py-3 px-4 font-medium text-gray-700">Available For</th>
                  <th className="text-right py-3 px-4 font-medium text-gray-700">Actions</th>
                </tr>
              </thead>
              <tbody>
                {departments.map((dept) => (
                  <tr key={dept.id} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4 font-medium text-gray-900">{dept.name}</td>
                    <td className="py-3 px-4 text-gray-600">{dept.code}</td>
                    <td className="py-3 px-4 text-gray-600">{dept.headOfDepartment || '-'}</td>
                    <td className="py-3 px-4">
                      <div className="flex gap-1 flex-wrap">
                        {(dept.years || '1,2,3,4').split(',').map((y: string) => (
                          <span key={y} className="text-xs px-2 py-0.5 bg-primary-100 text-primary-700 rounded">{yearLabels[parseInt(y.trim())]?.short}</span>
                        ))}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button onClick={() => openEditModal('department', dept)} className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"><FiEdit2 size={16} /></button>
                      <button onClick={() => handleDelete('department', dept.id, dept.name)} className="p-2 text-red-600 hover:bg-red-50 rounded-lg"><FiTrash2 size={16} /></button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {departments.length === 0 && (
              <div className="text-center py-8 text-gray-500">No departments yet for {selectedAcademicYear?.yearName}. Add your first department or copy from another year.</div>
            )}
          </div>
        </Card>
      </div>
    </>
  );

  const renderDepartmentDetail = () => {
    const filteredDivisions = getFilteredDivisions();
    const filteredCourses = getFilteredCourses();
    const semNum = (selectedYear! - 1) * 2 + (selectedSemester === 'ODD' ? 1 : 2);
    
    return (
      <>
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-4">
            <div className="flex bg-gray-100 rounded-lg p-1">
              <button onClick={() => setSelectedSemester('ODD')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${selectedSemester === 'ODD' ? 'bg-white text-primary-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'}`}>
                Semester {(selectedYear! - 1) * 2 + 1}
              </button>
              <button onClick={() => setSelectedSemester('EVEN')} className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${selectedSemester === 'EVEN' ? 'bg-white text-primary-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'}`}>
                Semester {(selectedYear! - 1) * 2 + 2}
              </button>
            </div>
            <span className="text-sm text-gray-500">{yearLabels[selectedYear!].full} • {selectedAcademicYear?.yearName}</span>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Card>
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2"><FiUsers size={18} /> Divisions</h3>
              <Button variant="primary" size="sm" onClick={() => openAddModal('division')}><FiPlus className="mr-1" /> Add</Button>
            </div>
            {filteredDivisions.length === 0 ? (
              <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-lg">No divisions yet</div>
            ) : (
              <div className="space-y-3">
                {filteredDivisions.map((div) => (
                  <div key={div.id} className="p-4 bg-gray-50 border rounded-lg hover:shadow-sm cursor-pointer" onClick={() => navigateTo('batches', div)}>
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 bg-primary-100 text-primary-600 rounded-lg flex items-center justify-center font-bold text-lg">{div.name}</div>
                        <div>
                          <span className="font-medium text-gray-900">{div.branch}</span>
                          <div className="flex gap-2 mt-1">
                            <span className="text-xs text-gray-500">{div.totalStudents} students</span>
                            <span className={`text-xs px-1.5 py-0.5 rounded ${div.timeSlotType === 'TYPE_2' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'}`}>
                              {scheduleLabels[div.timeSlotType] || 'Schedule 1'}
                            </span>
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                        <button onClick={() => openEditModal('division', div)} className="p-1.5 text-blue-600 hover:bg-blue-50 rounded"><FiEdit2 size={14} /></button>
                        <button onClick={() => handleDelete('division', div.id, div.name)} className="p-1.5 text-red-600 hover:bg-red-50 rounded"><FiTrash2 size={14} /></button>
                        <FiChevronRight className="text-gray-400 ml-1" />
                      </div>
                    </div>
                    {(div.classTeacher || div.classRepresentative) && (
                      <div className="mt-3 pt-3 border-t border-gray-200 grid grid-cols-2 gap-2 text-xs">
                        {div.classTeacher && <div><span className="text-gray-500">Class Teacher:</span> <span className="text-gray-700">{div.classTeacher}</span></div>}
                        {div.classRepresentative && <div><span className="text-gray-500">CR:</span> <span className="text-gray-700">{div.classRepresentative}</span></div>}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Card>

          <Card>
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-semibold text-gray-900 flex items-center gap-2"><FiBook size={18} /> Courses (Sem {semNum})</h3>
              <div className="flex gap-2">
                <Button variant="outline" size="sm" onClick={() => setShowUploadModal(true)}><FiUpload className="mr-1" /> Upload</Button>
                <Button variant="primary" size="sm" onClick={() => openAddModal('course')}><FiPlus className="mr-1" /> Add</Button>
              </div>
            </div>
            {filteredCourses.length === 0 ? (
              <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-lg">No courses for Semester {semNum}</div>
            ) : (
              <div className="space-y-2 max-h-96 overflow-y-auto">
                {filteredCourses.map((course) => (
                  <div key={course.id} className="flex items-center justify-between p-3 bg-gray-50 border rounded-lg">
                    <div>
                      <span className="font-medium text-gray-900">{course.name}</span>
                      <div className="flex gap-2 mt-0.5">
                        <span className="text-xs text-gray-500">{course.code}</span>
                        <span className={`text-xs px-1.5 py-0.5 rounded ${course.courseType === 'LAB' ? 'bg-green-100 text-green-700' : course.courseType === 'TUTORIAL' ? 'bg-yellow-100 text-yellow-700' : 'bg-blue-100 text-blue-700'}`}>{course.courseType}</span>
                        <span className="text-xs text-gray-500">{course.credits} cr • {course.hoursPerWeek} hrs/wk</span>
                      </div>
                    </div>
                    <div className="flex gap-1">
                      <button onClick={() => openEditModal('course', course)} className="p-1.5 text-blue-600 hover:bg-blue-50 rounded"><FiEdit2 size={14} /></button>
                      <button onClick={() => handleDelete('course', course.id, course.name)} className="p-1.5 text-red-600 hover:bg-red-50 rounded"><FiTrash2 size={14} /></button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </>
    );
  };

  const renderBatches = () => {
    const filteredBatches = getFilteredBatches();
    return (
      <>
        <div className="flex justify-between items-center mb-6">
          <p className="text-gray-600">{selectedDepartment?.name} • {yearLabels[selectedYear!].short} • Division {selectedDivision?.name}</p>
          <Button variant="primary" onClick={() => openAddModal('batch')} className="flex items-center gap-2"><FiPlus /> Add Batch</Button>
        </div>
        {filteredBatches.length === 0 ? (
          <div className="text-center py-12 text-gray-500 bg-gray-50 rounded-lg">No batches yet. Create your first batch.</div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
            {filteredBatches.map((batch) => (
              <div key={batch.id} className="flex items-center justify-between p-3 bg-white border rounded-lg">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 bg-amber-100 text-amber-600 rounded flex items-center justify-center"><FiGrid size={16} /></div>
                  <span className="font-medium">{batch.name}</span>
                </div>
                <div className="flex gap-1">
                  <button onClick={() => openEditModal('batch', batch)} className="p-1 text-blue-600 hover:bg-blue-50 rounded"><FiEdit2 size={12} /></button>
                  <button onClick={() => handleDelete('batch', batch.id, batch.name)} className="p-1 text-red-600 hover:bg-red-50 rounded"><FiTrash2 size={12} /></button>
                </div>
              </div>
            ))}
          </div>
        )}
      </>
    );
  };


  const renderModalContent = () => {
    switch (modalType) {
      case 'academic-year':
        return (
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input label="Year Name" value={formData.yearName} onChange={(e) => setFormData({ ...formData, yearName: e.target.value })} placeholder="2024-25" required />
            <div className="grid grid-cols-2 gap-4">
              <Input label="Start Date" type="date" value={formData.startDate} onChange={(e) => setFormData({ ...formData, startDate: e.target.value })} required />
              <Input label="End Date" type="date" value={formData.endDate} onChange={(e) => setFormData({ ...formData, endDate: e.target.value })} required />
            </div>
            <label className="flex items-center gap-2"><input type="checkbox" checked={formData.isCurrent} onChange={(e) => setFormData({ ...formData, isCurrent: e.target.checked })} className="w-4 h-4 text-primary-600 rounded" /><span className="text-sm text-gray-700">Current academic year</span></label>
            <div className="flex gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => setShowModal(false)} className="flex-1">Cancel</Button>
              <Button type="submit" variant="primary" isLoading={isLoading} className="flex-1">{editingItem ? 'Update' : 'Create'}</Button>
            </div>
          </form>
        );
      case 'department':
        return (
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input label="Department Name" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="Computer Engineering" required />
            <Input label="Code" value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })} placeholder="COMP" maxLength={10} required />
            <div className="relative">
              <Input 
                label="Head of Department" 
                value={formData.headOfDepartment} 
                onChange={(e) => {
                  setFormData({ ...formData, headOfDepartment: e.target.value });
                  setHodSuggestions(filterStaffSuggestions(e.target.value));
                  setShowHodSuggestions(true);
                }} 
                onFocus={() => setShowHodSuggestions(formData.headOfDepartment?.length >= 2)}
                onBlur={() => setTimeout(() => setShowHodSuggestions(false), 200)}
                placeholder="Start typing to search faculty..." 
                autoComplete="off"
              />
              {showHodSuggestions && hodSuggestions.length > 0 && (
                <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-40 overflow-y-auto">
                  {hodSuggestions.map((staff) => (
                    <button key={staff.id} type="button" onClick={() => { setFormData({ ...formData, headOfDepartment: staff.name }); setShowHodSuggestions(false); }} className="w-full text-left px-3 py-2 hover:bg-gray-100 text-sm">
                      <span className="font-medium">{staff.name}</span>
                      <span className="text-gray-500 ml-2">({staff.employeeId})</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Available for Years</label>
              <div className="flex gap-6 p-3 bg-gray-50 rounded-lg">
                {[1, 2, 3, 4].map((year) => (
                  <label key={year} className="flex items-center gap-2 cursor-pointer">
                    <input type="checkbox" checked={formData.years?.includes(year)} onChange={(e) => {
                      const newYears = e.target.checked ? [...(formData.years || []), year] : (formData.years || []).filter((y: number) => y !== year);
                      setFormData({ ...formData, years: newYears.sort() });
                    }} className="w-4 h-4 text-primary-600 rounded" />
                    <span className="text-sm text-gray-700">{yearLabels[year].short}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className="flex gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => setShowModal(false)} className="flex-1">Cancel</Button>
              <Button type="submit" variant="primary" isLoading={isLoading} className="flex-1">{editingItem ? 'Update' : 'Create'}</Button>
            </div>
          </form>
        );
      case 'division':
        return (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <Input label="Division Name" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value.toUpperCase() })} placeholder="A" maxLength={2} required />
              <Input label="Branch" value={formData.branch} onChange={(e) => setFormData({ ...formData, branch: e.target.value })} placeholder="Computer Science" required />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Input label="Total Students" type="number" value={formData.totalStudents} onChange={(e) => setFormData({ ...formData, totalStudents: e.target.value })} placeholder="60" />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Time Slot Schedule</label>
                <select value={formData.timeSlotType} onChange={(e) => setFormData({ ...formData, timeSlotType: e.target.value })} className="input-field">
                  <option value="TYPE_1">Schedule 1</option>
                  <option value="TYPE_2">Schedule 2</option>
                </select>
              </div>
            </div>
            <div className="relative">
              <Input 
                label="Class Teacher" 
                value={formData.classTeacher} 
                onChange={(e) => {
                  setFormData({ ...formData, classTeacher: e.target.value });
                  setTeacherSuggestions(filterStaffSuggestions(e.target.value));
                  setShowTeacherSuggestions(true);
                }} 
                onFocus={() => setShowTeacherSuggestions(formData.classTeacher?.length >= 2)}
                onBlur={() => setTimeout(() => setShowTeacherSuggestions(false), 200)}
                placeholder="Start typing to search faculty..." 
                autoComplete="off"
              />
              {showTeacherSuggestions && teacherSuggestions.length > 0 && (
                <div className="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-40 overflow-y-auto">
                  {teacherSuggestions.map((staff) => (
                    <button key={staff.id} type="button" onClick={() => { setFormData({ ...formData, classTeacher: staff.name }); setShowTeacherSuggestions(false); }} className="w-full text-left px-3 py-2 hover:bg-gray-100 text-sm">
                      <span className="font-medium">{staff.name}</span>
                      <span className="text-gray-500 ml-2">({staff.employeeId})</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <Input label="Class Representative (CR)" value={formData.classRepresentative} onChange={(e) => setFormData({ ...formData, classRepresentative: e.target.value })} placeholder="Student Name" />
            <div className="flex gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => setShowModal(false)} className="flex-1">Cancel</Button>
              <Button type="submit" variant="primary" isLoading={isLoading} className="flex-1">{editingItem ? 'Update' : 'Create'}</Button>
            </div>
          </form>
        );
      case 'course':
        return (
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input label="Course Name" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value })} placeholder="Data Structures" required />
            <div className="grid grid-cols-2 gap-4">
              <Input label="Course Code" value={formData.code} onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })} placeholder="CS201" required />
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Course Type</label>
                <select value={formData.courseType} onChange={(e) => setFormData({ ...formData, courseType: e.target.value })} className="input-field">
                  <option value="THEORY">Theory</option>
                  <option value="LAB">Lab</option>
                  <option value="TUTORIAL">Tutorial</option>
                </select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Input label="Credits" type="number" value={formData.credits} onChange={(e) => setFormData({ ...formData, credits: e.target.value })} placeholder="3" min="1" required />
              <Input label="Hours/Week" type="number" value={formData.hoursPerWeek} onChange={(e) => setFormData({ ...formData, hoursPerWeek: e.target.value })} placeholder="4" min="1" required />
            </div>
            <div className="bg-gray-50 p-3 rounded-lg text-sm text-gray-600">
              This course will be added to <strong>Semester {(selectedYear! - 1) * 2 + (selectedSemester === 'ODD' ? 1 : 2)}</strong> of {selectedDepartment?.name}
            </div>
            <div className="flex gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => setShowModal(false)} className="flex-1">Cancel</Button>
              <Button type="submit" variant="primary" isLoading={isLoading} className="flex-1">{editingItem ? 'Update' : 'Create'}</Button>
            </div>
          </form>
        );
      case 'batch':
        return (
          <form onSubmit={handleSubmit} className="space-y-4">
            <Input label="Batch Name" value={formData.name} onChange={(e) => setFormData({ ...formData, name: e.target.value.toUpperCase() })} placeholder="B1" required />
            <div className="flex gap-3 pt-4">
              <Button type="button" variant="outline" onClick={() => setShowModal(false)} className="flex-1">Cancel</Button>
              <Button type="submit" variant="primary" isLoading={isLoading} className="flex-1">{editingItem ? 'Update' : 'Create'}</Button>
            </div>
          </form>
        );
      default: return null;
    }
  };

  const getModalTitle = () => {
    const action = editingItem ? 'Edit' : 'Add';
    const types: { [key: string]: string } = { 'academic-year': 'Academic Year', 'department': 'Department', 'division': 'Division', 'course': 'Course', 'batch': 'Batch' };
    return `${action} ${types[modalType] || ''}`;
  };

  return (
    <div>
      <div className="mb-4">
        <h1 className="text-2xl font-bold text-gray-900">Academic Structure</h1>
      </div>
      {renderBreadcrumbs()}
      {currentLevel === 'academic-years' && renderAcademicYears()}
      {currentLevel === 'year-overview' && renderYearOverview()}
      {currentLevel === 'department-detail' && renderDepartmentDetail()}
      {currentLevel === 'batches' && renderBatches()}
      
      <Modal isOpen={showModal} onClose={() => setShowModal(false)} title={getModalTitle()}>{renderModalContent()}</Modal>
      
      <Modal isOpen={showUploadModal} onClose={() => { setShowUploadModal(false); setSelectedFile(null); }} title="Upload Courses">
        <div className="space-y-4">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-blue-900">Download CSV Template</p>
                <p className="text-sm text-blue-700">Use this template to format your course data</p>
              </div>
              <Button variant="outline" onClick={handleDownloadTemplate} className="flex items-center gap-2">
                <FiDownload /> Download
              </Button>
            </div>
          </div>
          
          <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
            <input type="file" accept=".csv" onChange={handleFileSelect} ref={fileInputRef} className="hidden" id="course-csv-upload" />
            <label htmlFor="course-csv-upload" className="cursor-pointer">
              <FiUpload className="mx-auto w-10 h-10 text-gray-400 mb-3" />
              <p className="text-gray-600">{selectedFile ? selectedFile.name : 'Click to select CSV file'}</p>
              <p className="text-sm text-gray-500 mt-1">Courses will be added to {selectedDepartment?.name} - {yearLabels[selectedYear!]?.short}</p>
            </label>
          </div>
          
          <div className="bg-gray-50 rounded-lg p-3 text-sm text-gray-600">
            <p className="font-medium mb-1">CSV Format:</p>
            <p>Name, Code, Type (THEORY/LAB/TUTORIAL), Credits, Hours Per Week, Semester (1-8)</p>
          </div>
          
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => { setShowUploadModal(false); setSelectedFile(null); }} className="flex-1">Cancel</Button>
            <Button variant="primary" onClick={handleCourseUpload} isLoading={isLoading} disabled={!selectedFile} className="flex-1">Upload Courses</Button>
          </div>
        </div>
      </Modal>

      {/* Copy Departments Modal */}
      <Modal isOpen={showCopyModal} onClose={() => setShowCopyModal(false)} title="Copy Departments from Another Year">
        <div className="space-y-4">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <p className="text-sm text-blue-800">
              Copy departments from another academic year to <strong>{selectedAcademicYear?.yearName}</strong>. 
              This will create new departments with the same details.
            </p>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Source Academic Year</label>
            <select 
              value={sourceAcademicYear?.id || ''} 
              onChange={(e) => handleSourceYearChange(Number(e.target.value))}
              className="input-field"
            >
              {academicYears
                .filter(y => y.id !== selectedAcademicYear?.id)
                .map(year => (
                  <option key={year.id} value={year.id}>{year.yearName}</option>
                ))
              }
            </select>
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Select Departments to Copy ({selectedDeptsToCopy.length} selected)
            </label>
            {sourceDepartments.length === 0 ? (
              <div className="text-center py-4 text-gray-500 bg-gray-50 rounded-lg">
                No departments in {sourceAcademicYear?.yearName}
              </div>
            ) : (
              <div className="max-h-60 overflow-y-auto border rounded-lg">
                <div className="p-2 border-b bg-gray-50">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input 
                      type="checkbox" 
                      checked={selectedDeptsToCopy.length === sourceDepartments.length && sourceDepartments.length > 0}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedDeptsToCopy(sourceDepartments.map(d => d.id));
                        } else {
                          setSelectedDeptsToCopy([]);
                        }
                      }}
                      className="w-4 h-4 text-primary-600 rounded"
                    />
                    <span className="text-sm font-medium text-gray-700">Select All</span>
                  </label>
                </div>
                {sourceDepartments.map(dept => (
                  <label key={dept.id} className="flex items-center gap-3 p-3 hover:bg-gray-50 cursor-pointer border-b last:border-b-0">
                    <input 
                      type="checkbox" 
                      checked={selectedDeptsToCopy.includes(dept.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedDeptsToCopy([...selectedDeptsToCopy, dept.id]);
                        } else {
                          setSelectedDeptsToCopy(selectedDeptsToCopy.filter(id => id !== dept.id));
                        }
                      }}
                      className="w-4 h-4 text-primary-600 rounded"
                    />
                    <div className="flex-1">
                      <span className="font-medium text-gray-900">{dept.name}</span>
                      <span className="text-gray-500 ml-2">({dept.code})</span>
                      <div className="flex gap-1 mt-1">
                        {(dept.years || '1,2,3,4').split(',').map((y: string) => (
                          <span key={y} className="text-xs px-1.5 py-0.5 bg-gray-100 text-gray-600 rounded">
                            {yearLabels[parseInt(y.trim())]?.short}
                          </span>
                        ))}
                      </div>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>
          
          <div className="flex gap-3 pt-2">
            <Button type="button" variant="outline" onClick={() => setShowCopyModal(false)} className="flex-1">Cancel</Button>
            <Button 
              variant="primary" 
              onClick={handleCopyDepartments} 
              isLoading={isLoading} 
              disabled={selectedDeptsToCopy.length === 0}
              className="flex-1"
            >
              Copy {selectedDeptsToCopy.length} Department(s)
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
