package it.polimi.tiw.imagegallery.controllers;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import it.polimi.tiw.imagegallery.dao.CommentDAO;
import it.polimi.tiw.imagegallery.utils.ConnectionManager;

@WebServlet("/PublishComment")
public class PublishComment extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	public PublishComment() {
		super();
	}
	
	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionManager.getConnection(servletContext);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String imageIdString = StringEscapeUtils.escapeJava(request.getParameter("selectedImageId"));
		String commentBody = StringEscapeUtils.escapeJava(request.getParameter("commentBody"));
		
		Integer userId = (Integer) request.getSession().getAttribute("userId");		
		if (userId == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user session");
			return;
		}
		
		try {
			if (imageIdString == null || imageIdString.isEmpty())
				throw new Exception("Missing image id");
			if (commentBody == null || commentBody.isBlank())
				throw new Exception("Missing or empty comment body");
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		int imageId;
		
		try {
			imageId = Integer.parseInt(imageIdString);
		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid image selection");
			return;
		}
		
		CommentDAO commentDAO = new CommentDAO(connection);
		
		try {
			commentDAO.createComment(userId, imageId, commentBody);
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to publish comment");
			return;
		}
		
		String path =	String.format(
				"/GoToAlbumPage?albumId=%s&page=%s&selectedImageId=%s",
				URLEncoder.encode(request.getParameter("albumId"), "UTF-8"),
				URLEncoder.encode(request.getParameter("page"), "UTF-8"),
				URLEncoder.encode(request.getParameter("selectedImageId"), "UTF-8"));
		response.sendRedirect(getServletContext().getContextPath() + path);
	}

	@Override
	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e){
				e.printStackTrace();
			}
		}
	}
}
