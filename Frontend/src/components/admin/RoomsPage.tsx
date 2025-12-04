import React, { useState, useEffect } from 'react';
import { FiPlus, FiEdit2, FiTrash2 } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Input } from '../common/Input';
import { roomAPI, departmentAPI } from '../../services/api';
import { Modal } from '../common/Modal';
import { getErrorMessage } from '../../utils/errorHandler';

export const RoomsPage: React.FC = () => {
  const [rooms, setRooms] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingRoom, setEditingRoom] = useState<any>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    roomNumber: '',
    buildingWing: '',
    capacity: '',
    roomType: 'classroom',
    departmentId: '',
    hasProjector: false,
    hasAc: false,
    equipment: '',
  });
  
  // Filter states
  const [searchQuery, setSearchQuery] = useState('');
  const [filterDepartment, setFilterDepartment] = useState('');
  const [filterWing, setFilterWing] = useState('');

  // Smart sync between room number and building wing
  const handleRoomNumberChange = (value: string) => {
    const upperValue = value.toUpperCase();
    setFormData(prev => {
      const newData = { ...prev, roomNumber: upperValue };
      
      // If room number starts with a letter, extract it as wing
      if (upperValue.length > 0 && /^[A-Z]/.test(upperValue)) {
        const firstLetter = upperValue.charAt(0);
        // Only auto-set wing if it's empty or matches the pattern
        if (!prev.buildingWing || prev.buildingWing === firstLetter) {
          newData.buildingWing = firstLetter;
        }
      }
      
      return newData;
    });
    setErrors({ ...errors, roomNumber: '' });
  };

  const handleBuildingWingChange = (value: string) => {
    const upperValue = value.toUpperCase();
    setFormData(prev => {
      const newData = { ...prev, buildingWing: upperValue };
      
      // If wing is set and room number is just digits, prepend the wing
      if (upperValue && prev.roomNumber && /^\d+$/.test(prev.roomNumber)) {
        newData.roomNumber = upperValue + prev.roomNumber;
      }
      // If wing is set and room number starts with different letter, replace it
      else if (upperValue && prev.roomNumber && /^[A-Z]\d/.test(prev.roomNumber)) {
        const digits = prev.roomNumber.substring(1);
        newData.roomNumber = upperValue + digits;
      }
      
      return newData;
    });
    setErrors({ ...errors, buildingWing: '' });
  };
  const [errors, setErrors] = useState<any>({});

  useEffect(() => {
    fetchRooms();
    fetchDepartments();
  }, []);

  const fetchRooms = async () => {
    try {
      const response = await roomAPI.getAll();
      setRooms(Array.isArray(response.data) ? response.data : []);
    } catch (error) {
      toast.error('Failed to fetch rooms');
      console.error(error);
      setRooms([]);
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

  const handleEdit = (room: any) => {
    setEditingRoom(room);
    setIsEditMode(true);
    setFormData({
      name: room.name,
      roomNumber: room.roomNumber,
      buildingWing: room.buildingWing || '',
      capacity: room.capacity.toString(),
      roomType: room.roomType.toLowerCase(),
      departmentId: room.department.id.toString(),
      hasProjector: room.hasProjector || false,
      hasAc: room.hasAc || false,
      equipment: room.equipment || '',
    });
    setShowModal(true);
  };

  const resetForm = () => {
    setFormData({
      name: '',
      roomNumber: '',
      buildingWing: '',
      capacity: '',
      roomType: 'classroom',
      departmentId: '',
      hasProjector: false,
      hasAc: false,
      equipment: '',
    });
    setEditingRoom(null);
    setIsEditMode(false);
    setErrors({});
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    const newErrors: any = {};
    if (!formData.name) newErrors.name = 'Name is required';
    if (!formData.roomNumber) newErrors.roomNumber = 'Room number is required';
    if (!formData.buildingWing) newErrors.buildingWing = 'Building wing is required';
    if (!formData.capacity) newErrors.capacity = 'Capacity is required';
    if (!formData.departmentId) newErrors.departmentId = 'Department is required';
    
    // Validate room number format (e.g., H202, A101, B305)
    const roomNumberPattern = /^[A-Z]\d{3}$/;
    if (formData.roomNumber && !roomNumberPattern.test(formData.roomNumber)) {
      newErrors.roomNumber = 'Room number must be in format: Letter + 3 digits (e.g., H202, A101)';
    }
    
    // Ensure room number starts with building wing
    if (formData.roomNumber && formData.buildingWing && !formData.roomNumber.startsWith(formData.buildingWing)) {
      newErrors.roomNumber = `Room number must start with wing "${formData.buildingWing}"`;
    }
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setIsLoading(true);
    try {
      const capacityValue = parseInt(formData.capacity, 10);
      const roomData = {
        name: formData.name,
        roomNumber: formData.roomNumber.toUpperCase(),
        buildingWing: formData.buildingWing,
        roomType: formData.roomType.toUpperCase(),
        capacity: capacityValue,
        hasProjector: formData.hasProjector,
        hasAc: formData.hasAc,
        equipment: formData.equipment || null,
        department: {
          id: parseInt(formData.departmentId, 10)
        },
      };
      
      if (isEditMode && editingRoom) {
        await roomAPI.update(editingRoom.id, roomData);
        toast.success('Room updated successfully!');
      } else {
        await roomAPI.create(roomData);
        toast.success('Room created successfully!');
      }
      
      setShowModal(false);
      resetForm();
      fetchRooms();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Room operation error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete ${name}?`)) {
      return;
    }

    try {
      await roomAPI.delete(id);
      toast.success('Room deleted successfully!');
      fetchRooms();
    } catch (error: any) {
      const errorMessage = getErrorMessage(error);
      toast.error(errorMessage, { duration: 5000 });
      console.error('Room deletion error:', error);
    }
  };

  const getRoomTypeColor = (type: string) => {
    switch (type.toUpperCase()) {
      case 'LAB': return 'bg-purple-100 text-purple-800';
      case 'AUDITORIUM': return 'bg-orange-100 text-orange-800';
      default: return 'bg-blue-100 text-blue-800';
    }
  };

  const getRoomTypeIcon = (type: string) => {
    switch (type.toUpperCase()) {
      case 'LAB': return '🔬';
      case 'AUDITORIUM': return '🎭';
      default: return '🏫';
    }
  };

  // Get unique wings from rooms
  const uniqueWings = Array.from(new Set(rooms.map(room => room.buildingWing).filter(Boolean)));

  // Filter rooms based on search and filters
  const filteredRooms = rooms.filter((room) => {
    const matchesSearch = searchQuery === '' || 
      room.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      room.roomNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      room.capacity.toString().includes(searchQuery);
    
    const matchesDepartment = filterDepartment === '' || 
      room.department.id.toString() === filterDepartment;
    
    const matchesWing = filterWing === '' || 
      room.buildingWing === filterWing;
    
    return matchesSearch && matchesDepartment && matchesWing;
  });

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Rooms</h1>
          <p className="text-gray-600 mt-1">Manage classrooms, labs, and auditoriums</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowModal(true);
          }}
          className="flex items-center gap-2"
        >
          <FiPlus /> Add Room
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
              placeholder="Search by name, number, or capacity..."
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
              Filter by Wing
            </label>
            <select
              value={filterWing}
              onChange={(e) => setFilterWing(e.target.value)}
              className="input-field"
            >
              <option value="">All Wings</option>
              {uniqueWings.sort().map((wing) => (
                <option key={wing} value={wing}>
                  Wing {wing}
                </option>
              ))}
            </select>
          </div>
        </div>
        {(searchQuery || filterDepartment || filterWing) && (
          <div className="mt-4 flex items-center justify-between text-sm">
            <span className="text-gray-600">
              Showing {filteredRooms.length} of {rooms.length} rooms
            </span>
            <button
              onClick={() => {
                setSearchQuery('');
                setFilterDepartment('');
                setFilterWing('');
              }}
              className="text-primary-600 hover:text-primary-700 font-medium"
            >
              Clear Filters
            </button>
          </div>
        )}
      </Card>

      {/* Rooms Grid */}
      {filteredRooms.length === 0 ? (
        <Card>
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">
              {rooms.length === 0 ? 'No rooms found. Create your first room to get started.' : 'No rooms match your filters.'}
            </p>
            {rooms.length > 0 && (
              <button
                onClick={() => {
                  setSearchQuery('');
                  setFilterDepartment('');
                  setFilterWing('');
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
          {filteredRooms.map((room) => (
          <Card key={room.id} hover>
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-start gap-3">
                <div className="text-3xl">
                  {getRoomTypeIcon(room.roomType)}
                </div>
                <div>
                  <h3 className="text-lg font-bold text-gray-900">{room.name}</h3>
                  <p className="text-sm text-gray-600">
                    Room {room.roomNumber}
                    {room.buildingWing && <span className="ml-2 text-primary-600">• Wing {room.buildingWing}</span>}
                  </p>
                </div>
              </div>
              <div className="flex gap-2">
                <button 
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                  onClick={() => handleEdit(room)}
                  title="Edit"
                >
                  <FiEdit2 size={18} />
                </button>
                <button 
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                  onClick={() => handleDelete(room.id, room.name)}
                >
                  <FiTrash2 size={18} />
                </button>
              </div>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Type:</span>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getRoomTypeColor(room.roomType)}`}>
                  {room.roomType}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Capacity:</span>
                <span className="font-medium">{room.capacity} students</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">Department:</span>
                <span className="font-medium text-xs">{room.department.name}</span>
              </div>
              <div className="flex gap-2 mt-3">
                {room.hasProjector && (
                  <span className="px-2 py-1 bg-green-100 text-green-800 rounded text-xs">
                    📽️ Projector
                  </span>
                )}
                {room.hasAc && (
                  <span className="px-2 py-1 bg-cyan-100 text-cyan-800 rounded text-xs">
                    ❄️ AC
                  </span>
                )}
              </div>
            </div>
          </Card>
          ))}
        </div>
      )}

      {/* Add/Edit Room Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => {
          setShowModal(false);
          resetForm();
        }}
        title={isEditMode ? "Edit Room" : "Add New Room"}
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Room Name"
              value={formData.name}
              onChange={(e) => {
                setFormData({ ...formData, name: e.target.value });
                setErrors({ ...errors, name: '' });
              }}
              error={errors.name}
              placeholder="Room 101"
            />

            <div>
              <Input
                label="Room Number"
                value={formData.roomNumber}
                onChange={(e) => handleRoomNumberChange(e.target.value)}
                error={errors.roomNumber}
                placeholder="H202 or 202"
              />
            </div>
          </div>

          <div>
            <Input
              label="Building/Wing"
              value={formData.buildingWing}
              onChange={(e) => handleBuildingWingChange(e.target.value)}
              error={errors.buildingWing}
              placeholder="H, A, B, C, etc."
              maxLength={10}
              required
            />
            <p className="text-xs text-gray-500 -mt-3">Auto-syncs with room number</p>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Room Type
              </label>
              <select
                value={formData.roomType}
                onChange={(e) => setFormData({ ...formData, roomType: e.target.value })}
                className="input-field"
              >
                <option value="classroom">Classroom</option>
                <option value="lab">Lab</option>
                <option value="auditorium">Auditorium</option>
              </select>
            </div>

            <Input
              label="Capacity"
              type="number"
              value={formData.capacity}
              onChange={(e) => {
                setFormData({ ...formData, capacity: e.target.value });
                setErrors({ ...errors, capacity: '' });
              }}
              error={errors.capacity}
              placeholder="60"
              min="1"
            />
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

          <div className="space-y-2">
            <label className="block text-sm font-medium text-gray-700">
              Facilities
            </label>
            <div className="flex gap-4">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={formData.hasProjector}
                  onChange={(e) => setFormData({ ...formData, hasProjector: e.target.checked })}
                  className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
                />
                <span className="text-sm text-gray-700">Projector</span>
              </label>
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={formData.hasAc}
                  onChange={(e) => setFormData({ ...formData, hasAc: e.target.checked })}
                  className="w-4 h-4 text-primary-600 rounded focus:ring-primary-500"
                />
                <span className="text-sm text-gray-700">Air Conditioning</span>
              </label>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Additional Equipment (Optional)
            </label>
            <textarea
              value={formData.equipment}
              onChange={(e) => setFormData({ ...formData, equipment: e.target.value })}
              className="input-field"
              rows={2}
              placeholder="Whiteboard, Smart board, etc."
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
              {isEditMode ? 'Update Room' : 'Create Room'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
