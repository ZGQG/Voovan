package org.voovan.http.server.router;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Date;

import org.voovan.http.server.HttpBizHandler;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.MimeTools;
import org.voovan.http.server.exception.ResourceNotFound;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;

/**
 * MIME 文件路由处理类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MimeFileRouter implements HttpBizHandler {

	private String	rootPath;

	public MimeFileRouter(String rootPath) {
		this.rootPath = rootPath;
	}

	@Override
	public void Process(HttpRequest request, HttpResponse response) throws Exception {
		String urlPath = request.protocol().getPath();
		if (MimeTools.isMimeFile(urlPath)) {
			// 获取扩展名
			String fileExtension = urlPath.substring(urlPath.lastIndexOf(".") + 1, urlPath.length());
			// 根据扩展名,设置 MIME 类型
			response.header().put("Content-Type", MimeTools.getMimeByFileExtension(fileExtension));
			// 转换请求Path 里的文件路劲分割符为系统默认分割符
			urlPath = urlPath.replaceAll("//", File.separator);
			// 拼装文件路径
			String filePath = rootPath + urlPath;
			File responseFile = new File(filePath);
			
			if (responseFile.exists()) {
				if(isNotModify(responseFile,request,response)){
					return ;
				}else{
					fillMimeFile(responseFile, request, response);
				}
			}else{
				throw new ResourceNotFound(urlPath);
			}
		}
	}
	
	/**
	 * 判断是否是304 not modify
	 * @param responseFile
	 * @param request
	 * @param response
	 * @return
	 * @throws ParseException
	 */
	public boolean isNotModify(File responseFile,HttpRequest request,HttpResponse response) throws ParseException{
		//文件的 ETag
		String eTag = "\"" + Integer.toString(responseFile.hashCode()) + "\"";
		
		//请求中的 ETag
		String requestETag = request.header().get("If-None-Match");
		
		//文件的修改日期
		Date fileModifyDate = new Date(responseFile.lastModified());
		
		//请求中的修改时间
		Date requestModifyDate = null;
		if(request.header().contain("If-Modified-Since")){
			requestModifyDate = TDateTime.parseToGMT(request.header().get("If-Modified-Since"));
		}
		
		//设置文件 hashCode
		response.header().put("ETag", "\"" + Integer.toString(responseFile.hashCode()) + "\"");
		//设置最后修改时间
		response.header().put("Last-Modified",TDateTime.formatToGMT(fileModifyDate));
		//设置缓存控制
		response.header().put("Cache-Control", "max-age=86400");
		//设置浏览器缓存超时控制
		response.header().put("Expires",TDateTime.formatToGMT(new Date(System.currentTimeMillis()+86400*1000)));
		
		//文件 hashcode 无变化,则返回304
		if(requestETag!=null && requestETag.equals(eTag)){
			setNotModifyResponse(response);
			return true;
		}
		//文件更新时间比请求时间大,则返回304
		if(requestModifyDate!=null && requestModifyDate.before(fileModifyDate)){
			setNotModifyResponse(response);
			return true; 
		} 
		return false;
	}
	
	/**
	 * 填充 mime 文件到 response
	 * @param responseFile
	 * @param request
	 * @param response
	 * @throws FileNotFoundException 
	 */
	public void fillMimeFile(File responseFile,HttpRequest request,HttpResponse response){
		byte[] fileByte = null;
		// 如果包含取一个范围内的文件内容进行处理,形似:Range: 0-800
		if (request.header().get("Range") != null && request.header().get("Range").contains("-")) {

			String rangeStr = request.header().get("Range");
			String[] ranges = rangeStr.split("-");
			if (ranges[1].equals("")) {
				ranges[1] = "-1";
			}
			// 是否是整形数字进行判断,如果不是整形数字,则全部使用-1
			int beginPos = TString.isInteger(ranges[0]) ? Integer.parseInt(ranges[0]) : -1;
			int endPos = TString.isInteger(ranges[1]) ? Integer.parseInt(ranges[1]) : -1;

			fileByte = TFile.loadFileFromSysPath(responseFile.getPath(), beginPos, endPos);
			response.header().put("Content-Range", "bytes " + rangeStr + "/" + fileByte.length);
		} else {
			fileByte = TFile.loadFileFromSysPath(responseFile.getPath());
		}

		if (fileByte != null) {
			response.header().put("Content-Length", "" + fileByte.length);
			response.write(fileByte);
		}
	}
	
	/**
	 * 将响应报文设置称304
	 * @param response
	 */
	public void setNotModifyResponse(HttpResponse response){
		response.protocol().setStatus(304);
		response.protocol().setStatusCode("Not Modified");
	}

}
