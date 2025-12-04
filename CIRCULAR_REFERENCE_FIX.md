# Circular Reference Fix - JSON Serialization

## Issue
After creating entities, the application was experiencing JSON serialization errors:
```
Document nesting depth (1001) exceeds the maximum allowed (1000)
```

This caused:
- Course creation to show errors (though entity was saved)
- All pages to fail loading after creation
- Department, Teacher, Course, Room pages showing errors

## Root Cause
**Bidirectional JPA relationships** creating infinite loops during JSON serialization:

```
CourseEntity → DepartmentEntity → courses → CourseEntity → DepartmentEntity → ...
```

When Jackson tries to serialize these entities to JSON for API responses, it follows all relationships infinitely, exceeding the maximum nesting depth of 1000 levels.

## Solution
Added `@JsonIgnore` annotations to **collection sides** of bidirectional relationships to break the circular references.

### Entities Fixed

#### 1. CourseEntity
```java
@ManyToMany(mappedBy = "courses")
@JsonIgnore  // ✅ Added
private Set<TeacherEntity> teachers = new HashSet<>();

@OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<TimetableEntry> timetableEntries;
```

#### 2. DepartmentEntity
```java
@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<TeacherEntity> teachers;

@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<CourseEntity> courses;

@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<ClassRoom> rooms;

@OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<Division> divisions;
```

#### 3. ClassRoom (Room Entity)
```java
@OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<TimetableEntry> timetableEntries;
```

#### 4. Division
```java
@OneToMany(mappedBy = "division", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<TimetableEntry> timetableEntries;

@OneToMany(mappedBy = "division", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<Student> students;
```

#### 5. TeacherEntity
```java
@ManyToMany
@JoinTable(...)
@JsonIgnore  // ✅ Added
private Set<CourseEntity> courses = new HashSet<>();

@OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<TeacherAvailability> availabilities;

@OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
@JsonIgnore  // ✅ Added
private List<TimetableEntry> timetableEntries;
```

## Strategy
We applied `@JsonIgnore` to:
- **All `@OneToMany` collections** (the "many" side)
- **All `@ManyToMany` collections** that are mapped by another entity
- **Collections that are not needed in API responses** (like timetableEntries, availabilities)

We kept:
- **`@ManyToOne` relationships** (the "one" side) - these are needed for API responses
- Example: `CourseEntity.department` is NOT ignored, so courses include department info

## Result
✅ Entities can now be serialized to JSON without infinite loops
✅ Course creation works without errors
✅ All admin pages load correctly after entity creation
✅ API responses include necessary relationship data (department info) but avoid circular references

## Testing Checklist
- ✅ Create course - no JSON serialization error
- ✅ Create room - no JSON serialization error
- ✅ Create division - no JSON serialization error
- ✅ View courses page after creation
- ✅ View departments page after creation
- ✅ View teachers page after creation
- ✅ View rooms page after creation
- ✅ All pages load without nesting depth errors

## Additional Fix: Hibernate Lazy Loading Proxies

### Issue
After adding `@JsonIgnore`, a new error appeared:
```
No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor
through reference chain: CourseEntity["department"]->DepartmentEntity$HibernateProxy["hibernateLazyInitializer"]
```

### Root Cause
Entities with `@ManyToOne(fetch = FetchType.LAZY)` relationships are loaded as Hibernate proxy objects. When Jackson tries to serialize these proxies, it encounters internal Hibernate properties (`hibernateLazyInitializer`, `handler`) that cannot be serialized.

### Solution
Added `@JsonIgnoreProperties` to all entities to ignore Hibernate's internal proxy properties:

```java
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CourseEntity {
    // ...
}
```

Applied to all entities:
- ✅ CourseEntity
- ✅ DepartmentEntity
- ✅ ClassRoom
- ✅ Division
- ✅ TeacherEntity
- ✅ AcadamicYear

## Alternative Solutions Considered

### 1. @JsonManagedReference / @JsonBackReference
- More explicit control over serialization direction
- Requires pairing on both sides of relationship
- Chosen solution is simpler for this use case

### 2. DTOs (Data Transfer Objects)
- Best practice for complex applications
- Separates persistence layer from API layer
- More work to implement
- Recommended for future refactoring

### 3. @JsonIdentityInfo
- Handles circular references by using object IDs
- Can still include all relationships in response
- May result in larger response payloads
