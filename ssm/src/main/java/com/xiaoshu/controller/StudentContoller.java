package com.xiaoshu.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.xiaoshu.config.util.ConfigUtil;
import com.xiaoshu.entity.Course;
import com.xiaoshu.entity.Operation;
import com.xiaoshu.entity.Role;
import com.xiaoshu.entity.Student;
import com.xiaoshu.entity.StudentVo;
import com.xiaoshu.entity.User;
import com.xiaoshu.service.OperationService;
import com.xiaoshu.service.RoleService;
import com.xiaoshu.service.StudentService;
import com.xiaoshu.service.UserService;
import com.xiaoshu.util.StringUtil;
import com.xiaoshu.util.TimeUtil;
import com.xiaoshu.util.WriterUtil;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;

@Controller
@RequestMapping("student")
public class StudentContoller extends LogController{
	static Logger logger = Logger.getLogger(StudentContoller.class);

	@Autowired
	private UserService userService;
	
	@Autowired
	private RoleService roleService ;
	
	@Autowired
	private StudentService studentService ;
	
	@Autowired
	private OperationService operationService;
	
	
	@RequestMapping("studentIndex")
	public String index(HttpServletRequest request,Integer menuid) throws Exception{
		List<Course> roleList = studentService.findCourseAll();
		List<Operation> operationList = operationService.findOperationIdsByMenuid(menuid);
		request.setAttribute("operationList", operationList);
		request.setAttribute("roleList", roleList);
		return "student";
	}
	
	
	@RequestMapping(value="userList",method=RequestMethod.POST)
	public void userList(StudentVo studentVo,HttpServletRequest request,HttpServletResponse response,String offset,String limit) throws Exception{
		try {
			String order = request.getParameter("order");
			String ordername = request.getParameter("ordername");
			Integer pageSize = StringUtil.isEmpty(limit)?ConfigUtil.getPageSize():Integer.parseInt(limit);
			Integer pageNum =  (Integer.parseInt(offset)/pageSize)+1;
			PageInfo<StudentVo> userList= studentService.findStudentVoPage(studentVo,pageNum,pageSize,ordername,order);
			
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("total",userList.getTotal() );
			jsonObj.put("rows", userList.getList());
	        WriterUtil.write(response,jsonObj.toString());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("用户展示错误",e);
			throw e;
		}
	}
	
	
	
	// 新增或修改
		@RequestMapping("reserveUser")
		public void reserveUser(HttpServletRequest request,Student student,HttpServletResponse response){
			Integer userId = student.getId();
			JSONObject result=new JSONObject();
			try {
				Student userName = studentService.findByName(student.getName());
				if (userId != null) {   // userId不为空 说明是修改
					if(userName == null || (userName!=null&&userName.getId().equals(userId))){
						studentService.updateStudent(student);
						result.put("success", true);
					}else{
						result.put("success", true);
						result.put("errorMsg", "该用户名被使用");
					}
					
				}else {   // 添加
					if(userName==null){  // 没有重复可以添加
						student.setCreatetime(new Date());
						studentService.addStudent(student);
						result.put("success", true);
					} else {
						result.put("success", true);
						result.put("errorMsg", "该用户名被使用");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("保存用户信息错误",e);
				result.put("success", true);
				result.put("errorMsg", "对不起，操作失败");
			}
			WriterUtil.write(response, result.toString());
		}
		// 新增或修改
		@RequestMapping("reserveCourse")
		public void reserveCourse(HttpServletRequest request,Course course,HttpServletResponse response){
			JSONObject result=new JSONObject();
			try {
					course.setCreatatime(new Date());;
					studentService.addCourse(course);
					
					Jedis jedis=new Jedis("127.0.0.1", 6379);
					course=studentService.selectOne(course);
					
					//jedis.set(person.getExpressName(),person.getId()+"");
					jedis.hset("课程", course.getId()+"" , course.getName());
					
					result.put("success", true);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("保存用户信息错误",e);
				result.put("success", true);
				result.put("errorMsg", "对不起，操作失败");
			}
			WriterUtil.write(response, result.toString());
		}
		@RequestMapping("outStudent")
		public void outStudent(HttpServletRequest request,HttpServletResponse response){
			JSONObject result=new JSONObject();
			try {
				//导出
				String time = TimeUtil.formatTime(new Date(), "yyyyMMddHHmmss");
			    String excelName = "人员信息"+time;
				
			    List<StudentVo> list = studentService.findStudentAll();
			    
				String[] handers = {"学生编号","姓名","年龄","所选课程","所属年纪","入校时间"};
				// 1导入硬盘
				ExportExcelToDisk(request,handers,list, excelName);
				
				
				result.put("success", true);
				result.put("errorMsg", "导出成功");
			} catch (Exception e) {
				e.printStackTrace();
				result.put("errorMsg", "对不起，导出失败");
			}
			
			WriterUtil.write(response, result.toString());
		}
		// 导出到硬盘
		@SuppressWarnings("resource")
		private void ExportExcelToDisk(HttpServletRequest request,
				String[] handers, List<StudentVo> list, String excleName) throws Exception {
			
			try {
				HSSFWorkbook wb = new HSSFWorkbook();//创建工作簿
				HSSFSheet sheet = wb.createSheet("操作记录备份");//第一个sheet
				HSSFRow rowFirst = sheet.createRow(0);//第一个sheet第一行为标题
				rowFirst.setHeight((short) 500);
				for (int i = 0; i < handers.length; i++) {
					sheet.setColumnWidth((short) i, (short) 4000);// 设置列宽
				}
				//写标题了
				for (int i = 0; i < handers.length; i++) {
				    //获取第一行的每一个单元格
				    HSSFCell cell = rowFirst.createCell(i);
				    //往单元格里面写入值
				    cell.setCellValue(handers[i]);
				}
				for (int i = 0;i < list.size(); i++) {
				    //获取list里面存在是数据集对象
					StudentVo Vo = list.get(i);
				    //创建数据行
				    HSSFRow row = sheet.createRow(i+1);
				    //设置对应单元格的值
				    row.setHeight((short)400);   // 设置每行的高度
				    //"序号","操作人","IP地址","操作时间","操作模块","操作类型","详情"
				    row.createCell(0).setCellValue(Vo.getCode());
				    row.createCell(1).setCellValue(Vo.getName());
				    row.createCell(2).setCellValue(Vo.getAge());
				    row.createCell(3).setCellValue(Vo.getCname());
				    row.createCell(4).setCellValue(Vo.getGrade());
				    row.createCell(5).setCellValue(TimeUtil.formatTime(Vo.getEntrytime(), "yyyy-MM-dd HH:mm:ss"));
				}
				//写出文件（path为文件路径含文件名）
					OutputStream os;
					File file = new File("E:\\"+File.separator+excleName+".xls");
					
					if (!file.exists()){//若此目录不存在，则创建之  
						file.createNewFile();  
						logger.debug("创建文件夹路径为："+ file.getPath());  
		            } 
					os = new FileOutputStream(file);
					wb.write(os);
					os.close();
				} catch (Exception e) {
					e.printStackTrace();
					throw e;
				}
		}
		@RequestMapping("bbCourse")
		public void bbPerson(HttpServletRequest request,HttpServletResponse response){
			JSONObject result=new JSONObject();
			try {
				
				List<Course> list = studentService.countStudent();
				
				
				result.put("success", true);
				result.put("data", list);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("删除用户信息错误",e);
				result.put("errorMsg", "对不起，删除失败");
			}
			WriterUtil.write(response, result.toString());
		}
}
