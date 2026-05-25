import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { SelectionForm } from '../../components/admin/timetable-export/SelectionForm';
import { WeeklyGridDisplay } from '../../components/admin/timetable-export/WeeklyGridDisplay';
import { ExportButtons } from '../../components/admin/timetable-export/ExportButtons';
import { Loading } from '../../components/common/Loading';
import { ViewType, Semester, TimetableData } from '../../types/timetableExport';
import api from '../../services/api';

/**
 * TimetableExportPage Component
 * 
 * Main page for timetable export functionality.
 * Allows admins to select filters and view/export timetables for professors or rooms.
 * 
 * Features:
 * - Cascading filter selection (academic year, semester, view type, entity)
 * - Automatic timetable data fetching when all filters are selected
 * - Weekly grid display of timetable entries
 * - PDF and Excel export functionality
 * - Comprehensive error handling and loading states
 * 
 * Requirements: 2.4, 2.5, 3.3, 3.4, 4.3, 4.4, 5.5, 6.5, 7.5, 8.5, 13.4, 13.5, 15.1-15.5
 */
export const TimetableExportPage: React.FC = () => {
  // Filter state
  const [academicYearId, setAcademicYearId] = useState<number | null>(null);
  const [semester, setSemester] = useState<Semester | null>(null);
  const [viewType, setViewType] = useState<ViewType | null>(null);
  const [entityId, setEntityId] = useState<number | null>(null);

  // Data and UI state
  const [timetableData, setTimetableData] = useState<TimetableData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch timetable data from the backend
   * Called automatically when all filters are selected
   */
  const fetchTimetableData = async () => {
    if (!academicYearId || !semester || !viewType || !entityId) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const response = await api.get('/api/timetable-export/view', {
        params: {
          academicYearId,
          semester,
          viewType,
          entityId,
        },
      });

      // Check if we have entries
      if (!response.data.entries || response.data.entries.length === 0) {
        const entityType = viewType === 'PROFESSOR' ? 'faculty' : 'room';
        setError(
          `No timetable entries found for this ${entityType} in the selected academic year and semester. ` +
          `Please ensure that division timetables where this ${entityType} is assigned have been created.`
        );
        setTimetableData(null);
      } else {
        setTimetableData(response.data);
        setError(null);
      }
    } catch (err: any) {
      console.error('Failed to fetch timetable data:', err);

      // Handle specific error cases
      if (err.response?.status === 404) {
        const entityType = viewType === 'PROFESSOR' ? 'professor' : 'room';
        setError(`${entityType.charAt(0).toUpperCase() + entityType.slice(1)} not found.`);
      } else if (err.response?.status === 403) {
        setError('You do not have permission to view this timetable.');
        toast.error('Access denied');
      } else if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else {
        setError('Failed to load timetable data. Please try again.');
      }

      setTimetableData(null);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Automatically fetch timetable data when all filters are selected
   * Clear data when filters change
   */
  useEffect(() => {
    if (academicYearId && semester && viewType && entityId) {
      fetchTimetableData();
    } else {
      // Clear timetable data when filters are incomplete
      setTimetableData(null);
      setError(null);
    }
  }, [academicYearId, semester, viewType, entityId]);

  return (
    <div className="container mx-auto p-6">
      {/* Page Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900">Timetable Export</h1>
        <p className="text-gray-600 mt-1">
          View and export timetables for faculty and rooms
        </p>
      </div>

      {/* Filter Selection Form */}
      <SelectionForm
        academicYearId={academicYearId}
        setAcademicYearId={setAcademicYearId}
        semester={semester}
        setSemester={setSemester}
        viewType={viewType}
        setViewType={setViewType}
        entityId={entityId}
        setEntityId={setEntityId}
      />

      {/* Loading State */}
      {loading && (
        <div className="flex justify-center items-center py-12">
          <Loading />
        </div>
      )}

      {/* Error State */}
      {!loading && error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 mb-6">
          <div className="flex items-start">
            <div className="flex-shrink-0">
              <svg
                className="h-5 w-5 text-red-400"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                  clipRule="evenodd"
                />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">Error</h3>
              <p className="mt-1 text-sm text-red-700">{error}</p>
              <button
                onClick={fetchTimetableData}
                className="mt-3 text-sm font-medium text-red-800 hover:text-red-900 underline"
              >
                Try again
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Timetable Display */}
      {!loading && !error && timetableData && viewType && (
        <>
          {/* Export Buttons */}
          <ExportButtons
            viewType={viewType}
            entityId={entityId!}
            academicYearId={academicYearId!}
            semester={semester!}
          />

          {/* Weekly Grid Display */}
          <WeeklyGridDisplay data={timetableData} viewType={viewType} />
        </>
      )}

      {/* Empty State - No filters selected */}
      {!loading && !error && !timetableData && (
        <div className="bg-gray-50 border border-gray-200 rounded-lg p-12 text-center">
          <svg
            className="mx-auto h-12 w-12 text-gray-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
            />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-gray-900">No timetable selected</h3>
          <p className="mt-1 text-sm text-gray-500">
            Select all filters above to view and export a timetable
          </p>
        </div>
      )}
    </div>
  );
};
