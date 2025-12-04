import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit2, FiTrash2, FiClock, FiCoffee } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { Modal } from '../common/Modal';
import { timeSlotAPI } from '../../services/api';
import { getErrorMessage } from '../../utils/errorHandler';

export const TimeSlotsPage: React.FC = () => {
  const [timeSlots, setTimeSlots] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingSlot, setEditingSlot] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    slotName: '',
    startTime: '',
    endTime: '',
    isBreak: false,
  });
  const [errors, setErrors] = useState<any>({});

  useEffect(() => {
    fetchTimeSlots();
  }, []);

  const fetchTimeSlots = async () => {
    try {
      const response = await timeSlotAPI.getAll();
      setTimeSlots(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      toast.error('Failed to fetch time slots');
      console.error(error);
      setTimeSlots([]);
    }
  };

  const handleEdit = (slot: any) => {
    setEditingSlot(slot);
    setIsEditMode(true);
    setFormData({
      slotName: slot.slotName,
      startTime: slot.startTime,
      endTime: slot.endTime,
      isBreak: slot.isBreak,
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({ slotName: '', startTime: '', endTime: '', isBreak: false });
    setEditingSlot(null);
    setIsEditMode(false);
    setErrors({});
  };

  const calculateDuration = (start: string, end: string) => {
    if (!start || !end) return 0;
    const [startHour, startMin] = start.split(':').map(Number);
    const [endHour, endMin] = end.split(':').map(Number);
    return (endHour * 60 + endMin) - (startHour * 60 + startMin);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.slotName) newErrors.slotName = 'Slot name is required';
    if (!formData.startTime) newErrors.startTime = 'Start time is required';
    if (!formData.endTime) newErrors.endTime = 'End time is required';
    
    if (formData.startTime && formData.endTime && formData.startTime >= formData.endTime) {
      newErrors.endTime = 'End time must be after start time';
    }
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      const duration = calculateDuration(formData.startTime, formData.endTime);
      const slotData = { ...formData, durationMinutes: duration };

      if (isEditMode && editingSlot) {
        await timeSlotAPI.update(editingSlot.id, slotData);
        toast.success('Time slot updated successfully!');
      } else {
        await timeSlotAPI.create(slotData);
        toast.success('Time slot created successfully!');
      }

      setShowModal(false);
      resetForm();
      fetchTimeSlots();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Time slot operation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number, slotName: string) => {
    if (!window.confirm(`Are you sure you want to delete ${slotName}?`)) {
      return;
    }

    try {
      await timeSlotAPI.delete(id);
      toast.success('Time slot deleted successfully!');
      fetchTimeSlots();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Time slot deletion error:', error);
    }
  };

  const formatTime = (time: string) => {
    const [hour, min] = time.split(':');
    const h = parseInt(hour);
    const ampm = h >= 12 ? 'PM' : 'AM';
    const displayHour = h > 12 ? h - 12 : h === 0 ? 12 : h;
    return `${displayHour}:${min} ${ampm}`;
  };

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Time Slots</h1>
          <p className="text-gray-600 mt-1">Manage class periods and break times</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Time Slot
        </Button>
      </div>

      {/* Time Slots List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {timeSlots.map((slot) => (
          <Card key={slot.id} hover className={slot.isBreak ? 'border-l-4 border-orange-500' : 'border-l-4 border-primary-500'}>
            <div className="flex justify-between items-start">
              <div className="flex items-start gap-3 flex-1">
                <div className={`p-2 rounded-lg ${slot.isBreak ? 'bg-orange-100 text-orange-600' : 'bg-primary-100 text-primary-600'}`}>
                  {slot.isBreak ? <FiCoffee size={20} /> : <FiClock size={20} />}
                </div>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="text-lg font-bold text-gray-900">{slot.slotName}</h3>
                    {slot.isBreak && (
                      <span className="px-2 py-0.5 bg-orange-100 text-orange-800 rounded text-xs font-medium">
                        Break
                      </span>
                    )}
                  </div>
                  <div className="mt-2 space-y-1 text-sm">
                    <p className="text-gray-600">
                      <span className="font-medium">Time:</span> {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
                    </p>
                    <p className="text-gray-600">
                      <span className="font-medium">Duration:</span> {slot.durationMinutes} minutes
                    </p>
                  </div>
                </div>
              </div>
              <div className="flex gap-2 ml-2">
                <button 
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                  onClick={() => handleEdit(slot)}
                  title="Edit"
                >
                  <FiEdit2 size={16} />
                </button>
                <button 
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                  onClick={() => handleDelete(slot.id, slot.slotName)}
                >
                  <FiTrash2 size={16} />
                </button>
              </div>
            </div>
          </Card>
        ))}
      </div>

      {/* Daily Schedule Preview */}
      <Card className="mt-6">
        <h3 className="text-xl font-bold text-gray-900 mb-4">Daily Schedule Preview</h3>
        <div className="space-y-2">
          {timeSlots.map((slot) => (
            <div key={slot.id} className="flex items-center gap-4">
              <div className="w-32 text-sm font-medium text-gray-700">
                {formatTime(slot.startTime)} - {formatTime(slot.endTime)}
              </div>
              <div className={`flex-1 p-3 rounded-lg ${slot.isBreak ? 'bg-orange-50 border border-orange-200' : 'bg-primary-50 border border-primary-200'}`}>
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900">{slot.slotName}</span>
                  <span className="text-sm text-gray-600">{slot.durationMinutes} min</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      {/* Add/Edit Time Slot Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Time Slot" : "Add New Time Slot"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            label="Slot Name"
            value={formData.slotName}
            onChange={(e) => {
              setFormData({ ...formData, slotName: e.target.value });
              setErrors({ ...errors, slotName: '' });
            }}
            error={errors.slotName}
            placeholder="Period 1 or Lunch Break"
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Start Time"
              type="time"
              value={formData.startTime}
              onChange={(e) => {
                setFormData({ ...formData, startTime: e.target.value });
                setErrors({ ...errors, startTime: '' });
              }}
              error={errors.startTime}
            />

            <Input
              label="End Time"
              type="time"
              value={formData.endTime}
              onChange={(e) => {
                setFormData({ ...formData, endTime: e.target.value });
                setErrors({ ...errors, endTime: '' });
              }}
              error={errors.endTime}
            />
          </div>

          {formData.startTime && formData.endTime && (
            <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg">
              <p className="text-sm text-blue-800">
                <span className="font-medium">Duration:</span> {calculateDuration(formData.startTime, formData.endTime)} minutes
              </p>
            </div>
          )}

          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="isBreak"
              checked={formData.isBreak}
              onChange={(e) => setFormData({ ...formData, isBreak: e.target.checked })}
              className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
            />
            <label htmlFor="isBreak" className="text-sm text-gray-700">
              This is a break period (cannot be scheduled for classes)
            </label>
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
              {isEditMode ? 'Update Time Slot' : 'Create Time Slot'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
