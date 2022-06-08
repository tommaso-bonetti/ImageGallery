package it.polimi.tiw.imagegallery.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.imagegallery.beans.Album;
import it.polimi.tiw.imagegallery.beans.Comment;
import it.polimi.tiw.imagegallery.beans.Image;
import it.polimi.tiw.imagegallery.dao.AlbumDAO;
import it.polimi.tiw.imagegallery.dao.CommentDAO;
import it.polimi.tiw.imagegallery.dao.ImageDAO;
import it.polimi.tiw.imagegallery.utils.ConnectionManager;

@WebServlet("/GoToAlbumPage")
public class GoToAlbumPage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public GoToAlbumPage() {
		super();
	}

	@Override
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		connection = ConnectionManager.getConnection(servletContext);

		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String albumIdString = StringEscapeUtils.escapeJava(request.getParameter("albumId"));
		String pageString = StringEscapeUtils.escapeJava(request.getParameter("page"));
		String selectedImageIdString = StringEscapeUtils.escapeJava(request.getParameter("selectedImageId"));
		
		Integer userId = (Integer) request.getSession().getAttribute("userId");		
		if (userId == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing user session");
			return;
		}
		
		int albumId;
		int page;
		
		try {
			albumId = Integer.parseInt(albumIdString);
			page = Integer.parseInt(pageString);
		} catch (NumberFormatException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid album or page id");
			return;
		}
		
		Album album = null;
		List<Image> images = null;
		Image selectedImage = null;
		List<Comment> selectedImageComments = null;
		
		AlbumDAO albumDAO = new AlbumDAO(connection);
		ImageDAO imageDAO = new ImageDAO(connection);
		CommentDAO commentDAO = new CommentDAO(connection);
		try {
			album = albumDAO.fetchAlbumById(albumId);
			images = imageDAO.fetchImagesByAlbum(albumId, page);
			if (images.isEmpty() && page > 1)
				throw new Exception();
			if (selectedImageIdString != null) {
				try {
					int selectedImageId = Integer.parseInt(selectedImageIdString);
					selectedImage = imageDAO.fetchImageById(selectedImageId, albumId);
					selectedImageComments = commentDAO.fetchCommentsByImage(selectedImageId);
				} catch (NumberFormatException e) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid image id");
					return;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in retrieving images from the database");
			return;
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid page selection");
			return;
		}
		
		if (album == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid album selection");
			return;
		} else if (selectedImageIdString != null && selectedImage == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid image selection");
			return;
		}
		
		List<Album> otherAlbums = null;
		
		if (selectedImage != null) {
			try {
				otherAlbums = albumDAO.fetchOtherAlbumsByUser(userId, albumId);
			} catch (Exception e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in retrieving albums from the database");
				return;
			}
		}

		// Redirect to the home page and add the albums to the parameters
		String path = "/WEB-INF/album.html";
		ServletContext servletContext = getServletContext();
		final WebContext context = new WebContext(request, response, servletContext, request.getLocale());
		boolean lastPage = images.size() < 6;
		context.setVariable("album", album);
		context.setVariable("images", (lastPage ? images : images.subList(0, 5)));
		context.setVariable("page", page);
		context.setVariable("lastPage", lastPage);
		context.setVariable("selectedImage", selectedImage);
		context.setVariable("comments", selectedImageComments);
		context.setVariable("otherAlbums", otherAlbums);
		templateEngine.process(path, context, response.getWriter());
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
