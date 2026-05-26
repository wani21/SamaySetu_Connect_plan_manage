import React, { useState, useEffect } from 'react';
import { divisionAPI, batchAPI, academicYearAPI } from '../../services/api';
import { Division, Batch, AcademicYear } from '../../types';
import { FiPlus, FiEdit2, FiTrash2, FiLayers, FiInfo } from 'react-icons/fi';
import toast from 'react-hot-toast';

export const BatchesPage: React.FC = () => {
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
  const [divisions, setDivisions] = useState<Division[]>([]);
  const [selectedDivisionId, setSelectedDivisionId] = useState<number | null>(null);
  const [batches, setBatches] = useState<Batch[]>([]);
  const [batchesLoading, setBatchesLoading] = useState(false);

  // Form states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingBatch, setEditingBatch] = useState<Batch | null>(null);
  const [batchName, setBatchName] = useState('');

  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    if (selectedYearId) {
      fetchDivisions();
    } else {
      setDivisions([]);
      setSelectedDivisionId(null);
    }
  }, [selectedYearId]);

  useEffect(() => {
    if (selectedDivisionId) {
      fetchBatches();
    } else {
      setBatches([]);
    }
  }, [selectedDivisionId]);

  const fetchInitialData = async () => {
    try {
      const yearsRes = await academicYearAPI.getAll();
      setAcademicYears(yearsRes.data || []);
      const currentYear = (yearsRes.data || []).find((y: AcademicYear) => y.isActive);
      if (currentYear) {
        setSelectedYearId(currentYear.id);
      } else if ((yearsRes.data || []).length > 0) {
        setSelectedYearId(yearsRes.data[0].id);
      }
    } catch (error) {
      toast.error('Failed to load academic years');
    }
  };

  const fetchDivisions = async () => {
    if (!selectedYearId) return;
    try {
      const res = await divisionAPI.getByAcademicYear(selectedYearId);
      setDivisions(res.data || []);
      if ((res.data || []).length > 0) {
        setSelectedDivisionId(res.data[0].id);
      } else {
        setSelectedDivisionId(null);
      }
    } catch (error) {
      toast.error('Failed to load divisions');
    }
  };

  const fetchBatches = async () => {
    if (!selectedDivisionId) return;
    setBatchesLoading(true);
    try {
      const res = await batchAPI.getByDivision(selectedDivisionId);
      setBatches(res.data || []);
    } catch (error) {
      toast.error('Failed to load batches');
    } finally {
      setBatchesLoading(false);
    }
  };

  const handleOpenAddModal = () => {
    setEditingBatch(null);
    setBatchName('');
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (batch: Batch) => {
    setEditingBatch(batch);
    setBatchName(batch.name);
    setIsModalOpen(true);
  };

  const handleSaveBatch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!batchName.trim() || !selectedDivisionId) {
      toast.error('Batch name is required');
      return;
    }

    try {
      if (editingBatch) {
        await batchAPI.update(editingBatch.id, {
          name: batchName.trim(),
          divisionId: selectedDivisionId
        });
        toast.success('Batch updated successfully');
      } else {
        await batchAPI.create({
          name: batchName.trim(),
          divisionId: selectedDivisionId
        });
        toast.success('Batch created successfully');
      }
      setIsModalOpen(false);
      fetchBatches();
    } catch (error) {
      toast.error('Failed to save batch');
    }
  };

  const handleDeleteBatch = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this batch? All associated timetable entries will be affected.')) {
      return;
    }
    try {
      await batchAPI.delete(id);
      toast.success('Batch deleted successfully');
      fetchBatches();
    } catch (error) {
      toast.error('Failed to delete batch');
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Batch Management</h1>
          <p className="text-gray-500 text-sm mt-1">
            Configure laboratory and tutorial scheduling batches for department divisions.
          </p>
        </div>

        <button
          onClick={handleOpenAddModal}
          disabled={!selectedDivisionId}
          className="flex items-center gap-2 px-4 py-2.5 bg-primary-800 text-white rounded-xl font-medium hover:bg-primary-900 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm"
        >
          <FiPlus size={18} />
          Create Batch
        </button>
      </div>

      {/* Selectors Panel */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div className="space-y-1">
          <label className="text-sm font-semibold text-gray-600">Select Academic Year</label>
          <select
            value={selectedYearId || ''}
            onChange={(e) => setSelectedYearId(Number(e.target.value))}
            className="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:outline-none focus:border-primary-500"
          >
            {academicYears.map((y) => (
              <option key={y.id} value={y.id}>{y.name}</option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-sm font-semibold text-gray-600">Select Division</label>
          <select
            value={selectedDivisionId || ''}
            onChange={(e) => setSelectedDivisionId(Number(e.target.value))}
            disabled={divisions.length === 0}
            className="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:outline-none focus:border-primary-500 disabled:bg-gray-50"
          >
            {divisions.length === 0 ? (
              <option>No divisions available</option>
            ) : (
              divisions.map((d) => (
                <option key={d.id} value={d.id}>{d.name} (Year {d.year})</option>
              ))
            )}
          </select>
        </div>
      </div>

      {/* Batches Table / Grid */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        {!selectedDivisionId ? (
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <FiLayers className="text-gray-300 mb-4" size={48} />
            <h3 className="text-lg font-bold text-gray-700">No Division Selected</h3>
            <p className="text-gray-500 text-sm max-w-sm mt-1">
              Select an academic year and a division from the selectors panel to display and manage the laboratory batches.
            </p>
          </div>
        ) : batchesLoading ? (
          <div className="p-12 text-center flex flex-col items-center justify-center">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-800 mb-4"></div>
            <p className="text-gray-500 text-sm">Loading batches...</p>
          </div>
        ) : batches.length === 0 ? (
          <div className="flex flex-col items-center justify-center p-12 text-center">
            <FiInfo className="text-gray-300 mb-4" size={48} />
            <h3 className="text-lg font-bold text-gray-700">No Batches Configured</h3>
            <p className="text-gray-500 text-sm max-w-sm mt-1">
              There are no batches created for this division yet. Click the "Create Batch" button to get started.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="px-6 py-4 font-bold text-gray-600">Batch ID</th>
                  <th className="px-6 py-4 font-bold text-gray-600">Batch Name</th>
                  <th className="px-6 py-4 font-bold text-gray-600">Associated Division</th>
                  <th className="px-6 py-4 font-bold text-gray-600 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {batches.map((batch) => (
                  <tr key={batch.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-6 py-4 font-semibold text-gray-700">#{batch.id}</td>
                    <td className="px-6 py-4 font-semibold text-gray-900">{batch.name}</td>
                    <td className="px-6 py-4 text-gray-600">
                      {divisions.find(d => d.id === selectedDivisionId)?.name || 'Division'}
                    </td>
                    <td className="px-6 py-4 text-right flex justify-end gap-3">
                      <button
                        onClick={() => handleOpenEditModal(batch)}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="Edit Batch"
                      >
                        <FiEdit2 size={18} />
                      </button>
                      <button
                        onClick={() => handleDeleteBatch(batch.id)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete Batch"
                      >
                        <FiTrash2 size={18} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add/Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between">
              <h2 className="text-xl font-bold text-gray-800">
                {editingBatch ? 'Edit Batch' : 'Create Batch'}
              </h2>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-gray-400 hover:text-gray-600 text-2xl font-semibold focus:outline-none"
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSaveBatch} className="p-6 space-y-4">
              <div className="space-y-1">
                <label className="text-sm font-semibold text-gray-600">Batch Name</label>
                <input
                  type="text"
                  placeholder="e.g. Batch A1"
                  value={batchName}
                  onChange={(e) => setBatchName(e.target.value)}
                  className="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:outline-none focus:border-primary-500"
                  required
                />
              </div>

              <div className="pt-4 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 py-2.5 border border-gray-200 rounded-xl text-gray-700 font-medium hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2.5 bg-primary-800 text-white rounded-xl font-medium hover:bg-primary-900 transition-colors shadow-sm"
                >
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
