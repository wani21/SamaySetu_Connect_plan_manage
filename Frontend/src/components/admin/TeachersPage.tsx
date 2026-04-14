import React from 'react';
import { FiPlus } from 'react-icons/fi';
import { Card } from '../common/Card';
import { Button } from '../common/Button';

export const TeachersPage: React.FC = () => {
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Teachers</h1>
          <p className="text-gray-600 mt-1">Manage teaching faculty</p>
        </div>
        <Button variant="primary" className="flex items-center gap-2">
          <FiPlus /> Add Teacher
        </Button>
      </div>

      <Card>
        <p className="text-gray-600">Teachers management coming soon...</p>
      </Card>
    </div>
  );
};
