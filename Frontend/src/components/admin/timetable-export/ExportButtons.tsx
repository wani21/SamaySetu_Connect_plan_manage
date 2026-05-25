import React, { useState } from 'react';
import { FiDownload } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { ExportButtonsProps } from '../../../types/timetableExport';
import api from '../../../services/api';

/**
 * ExportButtons Component
 * 
 * Provides PDF and Excel export functionality for timetables.
 * Handles file download with proper content types and filenames.
 * 
 * Requirements: 11.1, 11.7, 12.1, 12.7, 15.4
 */
export const ExportButtons: React.FC<ExportButtonsProps> = ({
  viewType,
  entityId,
  academicYearId,
  semester,
}) => {
  const [isExportingPDF, setIsExportingPDF] = useState(false);
  const [isExportingExcel, setIsExportingExcel] = useState(false);

  /**
   * Extract filename from Content-Disposition header
   * Falls back to default filename if header is not present
   */
  const extractFilename = (contentDisposition: string | undefined, defaultName: string): string => {
    if (!contentDisposition) return defaultName;
    
    // Try to extract filename from Content-Disposition header
    // Format: attachment; filename="filename.ext"
    const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
    if (filenameMatch && filenameMatch[1]) {
      return filenameMatch[1].replace(/['"]/g, '');
    }
    
    return defaultName;
  };

  /**
   * Trigger browser download for a blob
   */
  const triggerDownload = (blob: Blob, filename: string) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    
    // Cleanup
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  /**
   * Handle PDF export
   */
  const handleExportPDF = async () => {
    try {
      setIsExportingPDF(true);
      
      // Determine endpoint based on view type
      const endpoint = viewType === 'PROFESSOR'
        ? `/api/timetable-export/professor/${entityId}/pdf`
        : `/api/timetable-export/room/${entityId}/pdf`;
      
      // Make API request with blob response type
      const response = await api.get(endpoint, {
        params: {
          academicYearId,
          semester,
        },
        responseType: 'blob',
      });
      
      // Extract filename from response headers
      const contentDisposition = response.headers['content-disposition'];
      const defaultFilename = `timetable_${viewType.toLowerCase()}_${entityId}_${semester}.pdf`;
      const filename = extractFilename(contentDisposition, defaultFilename);
      
      // Create blob and trigger download
      const blob = new Blob([response.data], { type: 'application/pdf' });
      triggerDownload(blob, filename);
      
      toast.success('PDF downloaded successfully!');
    } catch (error: any) {
      console.error('PDF export error:', error);
      
      if (error.response?.status === 404) {
        toast.error('Timetable not found');
      } else if (error.response?.status === 403) {
        toast.error('You do not have permission to export this timetable');
      } else {
        toast.error('Failed to export PDF. Please try again.');
      }
    } finally {
      setIsExportingPDF(false);
    }
  };

  /**
   * Handle Excel export
   */
  const handleExportExcel = async () => {
    try {
      setIsExportingExcel(true);
      
      // Determine endpoint based on view type
      const endpoint = viewType === 'PROFESSOR'
        ? `/api/timetable-export/professor/${entityId}/excel`
        : `/api/timetable-export/room/${entityId}/excel`;
      
      // Make API request with blob response type
      const response = await api.get(endpoint, {
        params: {
          academicYearId,
          semester,
        },
        responseType: 'blob',
      });
      
      // Extract filename from response headers
      const contentDisposition = response.headers['content-disposition'];
      const defaultFilename = `timetable_${viewType.toLowerCase()}_${entityId}_${semester}.xlsx`;
      const filename = extractFilename(contentDisposition, defaultFilename);
      
      // Create blob and trigger download
      const blob = new Blob([response.data], { 
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
      });
      triggerDownload(blob, filename);
      
      toast.success('Excel downloaded successfully!');
    } catch (error: any) {
      console.error('Excel export error:', error);
      
      if (error.response?.status === 404) {
        toast.error('Timetable not found');
      } else if (error.response?.status === 403) {
        toast.error('You do not have permission to export this timetable');
      } else {
        toast.error('Failed to export Excel. Please try again.');
      }
    } finally {
      setIsExportingExcel(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-4 mb-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">Export Timetable</h3>
          <p className="text-sm text-gray-600 mt-1">
            Download the timetable in your preferred format
          </p>
        </div>
        
        <div className="flex gap-3">
          {/* PDF Export Button */}
          <button
            onClick={handleExportPDF}
            disabled={isExportingPDF || isExportingExcel}
            className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            <FiDownload size={18} />
            <span>{isExportingPDF ? 'Exporting...' : 'Export PDF'}</span>
          </button>
          
          {/* Excel Export Button */}
          <button
            onClick={handleExportExcel}
            disabled={isExportingPDF || isExportingExcel}
            className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
          >
            <FiDownload size={18} />
            <span>{isExportingExcel ? 'Exporting...' : 'Export Excel'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
