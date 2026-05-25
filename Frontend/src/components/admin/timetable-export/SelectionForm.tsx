import React, { useState, useEffect } from 'react';
import { FiCalendar, FiBook, FiUser, FiMapPin } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { SelectionFormProps, AcademicYear, Semester, Professor, Room } from '../../../types/timetableExport';
import { academicYearAPI, teacherAPI, roomAPI } from '../../../services/api';

/**
 * SelectionForm Component
 * 
 * Provides cascading dropdowns for filtering timetable export:
 * 1. Academic Year (fetched from API)
 * 2. Semester (SEM_1, SEM_2)
 * 3. View Type (Faculty/Room)
 * 4. Entity Selection (Faculty or Room based on view type)
 * 
 * Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.5, 4.1, 4.2, 4.5, 5.1-5.4, 6.1-6.4
 */
export const SelectionForm: React.FC<SelectionFormProps> = ({
  academicYearId,
  setAcademicYearId,
  semester,
  setSemester,
  viewType,
  setViewType,
  entityId,
  setEntityId,
}) => {
  // State for academic years
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [isLoadingYears, setIsLoadingYears] = useState(true);

  // State for faculty
  const [professors, setProfessors] = useState<Professor[]>([]);
  const [isLoadingProfessors, setIsLoadingProfessors] = useState(false);

  // State for rooms
  const [rooms, setRooms] = useState<Room[]>([]);
  const [isLoadingRooms, setIsLoadingRooms] = useState(false);

  // Fetch academic years on component mount
  useEffect(() => {
    const fetchAcademicYears = async () => {
      try {
        setIsLoadingYears(true);
        const response = await academicYearAPI.getAll();
        const years = Array.isArray(response.data) ? response.data : [];
        
        // Sort by start_date descending (most recent first)
        const sortedYears = years.sort((a: AcademicYear, b: AcademicYear) => {
          const dateA = a.startDate ? new Date(a.startDate).getTime() : 0;
          const dateB = b.startDate ? new Date(b.startDate).getTime() : 0;
          return dateB - dateA; // Descending order
        });
        
        setAcademicYears(sortedYears);
        
        // Set current academic year as default
        const currentYear = sortedYears.find((year: AcademicYear) => year.isCurrent);
        if (currentYear && !academicYearId) {
          setAcademicYearId(currentYear.id);
        }
      } catch (error) {
        toast.error('Failed to load academic years');
        setAcademicYears([]);
      } finally {
        setIsLoadingYears(false);
      }
    };

    fetchAcademicYears();
  }, []); // Only run on mount

  // Fetch faculty when view type is PROFESSOR
  useEffect(() => {
    if (viewType === 'PROFESSOR') {
      const fetchProfessors = async () => {
        try {
          setIsLoadingProfessors(true);
          const response = await teacherAPI.getAll();
          const allProfessors = Array.isArray(response.data) ? response.data : [];
          
          // Filter only active faculty and sort alphabetically by name
          const activeProfessors = allProfessors
            .filter((prof: Professor) => prof.isActive)
            .sort((a: Professor, b: Professor) => a.name.localeCompare(b.name));
          
          setProfessors(activeProfessors);
        } catch (error) {
          toast.error('Failed to load faculty');
          setProfessors([]);
        } finally {
          setIsLoadingProfessors(false);
        }
      };

      fetchProfessors();
    }
  }, [viewType]);

  // Fetch rooms when view type is ROOM
  useEffect(() => {
    if (viewType === 'ROOM') {
      const fetchRooms = async () => {
        try {
          setIsLoadingRooms(true);
          const response = await roomAPI.getAll();
          const allRooms = Array.isArray(response.data) ? response.data : [];
          
          // Filter only active rooms and sort alphabetically by name
          const activeRooms = allRooms
            .filter((room: Room) => room.isActive)
            .sort((a: Room, b: Room) => a.name.localeCompare(b.name));
          
          setRooms(activeRooms);
        } catch (error) {
          toast.error('Failed to load rooms');
          setRooms([]);
        } finally {
          setIsLoadingRooms(false);
        }
      };

      fetchRooms();
    }
  }, [viewType]);

  // Reset downstream selections when academic year changes
  useEffect(() => {
    setSemester(null);
    setViewType(null);
    setEntityId(null);
  }, [academicYearId]);

  // Reset downstream selections when semester changes
  useEffect(() => {
    setViewType(null);
    setEntityId(null);
  }, [semester]);

  // Reset entity selection when view type changes
  useEffect(() => {
    setEntityId(null);
  }, [viewType]);

  // Handle academic year change
  const handleAcademicYearChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value;
    setAcademicYearId(value ? Number(value) : null);
  };

  // Handle semester change
  const handleSemesterChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value as Semester;
    setSemester(value || null);
  };

  // Handle view type change
  const handleViewTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value as 'PROFESSOR' | 'ROOM';
    setViewType(value || null);
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6 mb-6">
      <h2 className="text-xl font-semibold text-gray-900 mb-4">Filter Timetable</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Academic Year Dropdown */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <FiCalendar className="inline mr-1" />
            Academic Year <span className="text-red-500">*</span>
          </label>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
            value={academicYearId || ''}
            onChange={handleAcademicYearChange}
            disabled={isLoadingYears}
          >
            <option value="">
              {isLoadingYears ? 'Loading...' : 'Select Academic Year'}
            </option>
            {academicYears.map((year) => (
              <option key={year.id} value={year.id}>
                {year.yearName} {year.isCurrent ? '(Current)' : ''}
              </option>
            ))}
          </select>
        </div>

        {/* Semester Dropdown */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <FiBook className="inline mr-1" />
            Semester <span className="text-red-500">*</span>
          </label>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
            value={semester || ''}
            onChange={handleSemesterChange}
            disabled={!academicYearId}
          >
            <option value="">Select Semester</option>
            <option value={Semester.SEM_1}>Semester 1</option>
            <option value={Semester.SEM_2}>Semester 2</option>
          </select>
          {!academicYearId && (
            <p className="text-xs text-gray-500 mt-1">Select academic year first</p>
          )}
        </div>

        {/* View Type Dropdown */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            <FiUser className="inline mr-1" />
            View Type <span className="text-red-500">*</span>
          </label>
          <select
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
            value={viewType || ''}
            onChange={handleViewTypeChange}
            disabled={!semester}
          >
            <option value="">Select View Type</option>
            <option value="PROFESSOR">Faculty</option>
            <option value="ROOM">Room</option>
          </select>
          {!semester && (
            <p className="text-xs text-gray-500 mt-1">Select semester first</p>
          )}
        </div>

        {/* Entity Selection Dropdown */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            {viewType === 'PROFESSOR' ? (
              <><FiUser className="inline mr-1" />Faculty</>
            ) : viewType === 'ROOM' ? (
              <><FiMapPin className="inline mr-1" />Room</>
            ) : (
              <><FiUser className="inline mr-1" />Entity</>
            )}
            <span className="text-red-500"> *</span>
          </label>
          
          {/* Faculty Dropdown with Built-in Search */}
          {viewType === 'PROFESSOR' && (
            <div className="relative">
              <select
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                value={entityId || ''}
                onChange={(e) => setEntityId(e.target.value ? Number(e.target.value) : null)}
                disabled={!viewType || isLoadingProfessors}
              >
                <option value="">
                  {isLoadingProfessors 
                    ? 'Loading faculty...' 
                    : professors.length === 0 
                    ? 'No faculty found' 
                    : 'Select Faculty'}
                </option>
                {professors.map((prof) => (
                  <option key={prof.id} value={prof.id}>
                    {prof.name} ({prof.employeeId})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Room Dropdown with Built-in Search */}
          {viewType === 'ROOM' && (
            <div className="relative">
              <select
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                value={entityId || ''}
                onChange={(e) => setEntityId(e.target.value ? Number(e.target.value) : null)}
                disabled={!viewType || isLoadingRooms}
              >
                <option value="">
                  {isLoadingRooms 
                    ? 'Loading rooms...' 
                    : rooms.length === 0 
                    ? 'No rooms found' 
                    : 'Select Room'}
                </option>
                {rooms.map((room) => (
                  <option key={room.id} value={room.id}>
                    {room.name} ({room.roomNumber})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Placeholder when no view type selected */}
          {!viewType && (
            <>
              <select
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
                disabled={true}
              >
                <option value="">Select Entity</option>
              </select>
              <p className="text-xs text-gray-500 mt-1">Select view type first</p>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
