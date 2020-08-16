package com.xiaoshu.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.util.StringUtil;
import com.xiaoshu.dao.CourseMapper;
import com.xiaoshu.dao.StudentMapper;
import com.xiaoshu.entity.Course;
import com.xiaoshu.entity.Student;
import com.xiaoshu.entity.StudentVo;
import com.xiaoshu.entity.User;
import com.xiaoshu.entity.UserExample;
import com.xiaoshu.entity.UserExample.Criteria;

@Service
public class StudentService {

	@Autowired
	StudentMapper studentMapper;
	
	@Autowired
	CourseMapper courseMapper;

	public List<Course> findCourseAll() {
		// TODO Auto-generated method stub
		return courseMapper.selectAll();
	}

	public PageInfo<StudentVo> findStudentVoPage(StudentVo studentVo, Integer pageNum, Integer pageSize,
			String ordername, String order) {
		PageHelper.startPage(pageNum, pageSize);
		List<StudentVo> userList = studentMapper.findStudentVoAll(studentVo);
		PageInfo<StudentVo> pageInfo = new PageInfo<StudentVo>(userList);
		return pageInfo;
	}

	public void addCourse(Course course) {
		// TODO Auto-generated method stub
		courseMapper.insert(course);
	}

	public Course selectOne(Course course) {
		// TODO Auto-generated method stub
		return courseMapper.selectOne(course);
	}

	public Student findByName(String name) {
		// TODO Auto-generated method stub
		Student student = new Student();
		student.setName(name);
		
		return studentMapper.selectOne(student);
	}

	public void addStudent(Student student) {
		// TODO Auto-generated method stub
		studentMapper.insert(student);
	}

	public void updateStudent(Student student) {
		// TODO Auto-generated method stub
		studentMapper.updateByPrimaryKey(student);
	}

	public List<Course> countStudent() {
		// TODO Auto-generated method stub
		return studentMapper.countStudent();
	}

	public List<StudentVo> findStudentAll() {
		// TODO Auto-generated method stub
		return studentMapper.findStudentVoAll(null);
	}
	
}
