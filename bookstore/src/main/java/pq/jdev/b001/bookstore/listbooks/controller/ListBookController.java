package pq.jdev.b001.bookstore.listbooks.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.PagedListHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pq.jdev.b001.bookstore.books.model.Book;
import pq.jdev.b001.bookstore.listbooks.service.ListBookService;
import pq.jdev.b001.bookstore.users.model.Person;
import pq.jdev.b001.bookstore.users.service.UserService;

@Controller
public class ListBookController {
	@Autowired
	private ListBookService listBookService;
	
	@Autowired
	private UserService userService;

	@GetMapping("/book")
	public String index(Authentication authentication, ModelMap map, Model model, HttpServletRequest request,
			RedirectAttributes redirect, Principal principal) {

		if (authentication != null) {
			Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
			List<String> roles = new ArrayList<String>();
			for (GrantedAuthority a : authorities) {
				roles.add(a.getAuthority());
			}
			if (isUser(roles)) {
				map.addAttribute("header", "header_user");
				map.addAttribute("footer", "footer_user");
			} else if (isAdmin(roles)) {
				map.addAttribute("header", "header_admin");
				map.addAttribute("footer", "footer_admin");
			}
		} else {
			map.addAttribute("header", "header_login");
			map.addAttribute("footer", "footer_login");
		}
		
		PagedListHolder<?> pages = null;

		int pagesize = 4;
		List<Book> listH = null;
		if (principal == null) {
			listH = (List<Book>) listBookService.findAll();
		} else {
			Person per = userService.findByUsername(principal.getName());
			listH = getList(per);
		}

		if (pages == null) {
			pages = new PagedListHolder<>(listH);
			pages.setPageSize(pagesize);
		} 
		
		if (principal == null)
			request.getSession().setAttribute("bookListCC", pages);
		else
			request.getSession().setAttribute("bookListCR", pages);
		
		if (model.asMap().get("success") != null)
			redirect.addFlashAttribute("success", model.asMap().get("success").toString());
		return "redirect:/book/page/1";

	}

	// tao list
	@ModelAttribute("list")
	public List<Book> getList(Person p) {
		List<Book> oldList = listBookService.findAll();
		List<Book> newList = new ArrayList<Book>();
		Long id = p.getId();
		for (Book b : oldList) {
			b.setOk(0);
			if (b.getPerson().getId() == id)
				b.setOk(1);
			
			newList.add(b);
		}
		return newList;
	}

	@GetMapping("/book/page/{pageNumber}")
	public String showBookPage(Authentication authentication, HttpServletRequest request, @PathVariable int pageNumber,
			Model model, ModelMap map, Principal principal) {

		if (authentication != null) {
			Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
			List<String> roles = new ArrayList<String>();
			for (GrantedAuthority a : authorities) {
				roles.add(a.getAuthority());
			}
			
			if (isUser(roles)) {
				map.addAttribute("header", "header_user");
				map.addAttribute("footer", "footer_user");
				map.addAttribute("ok", "FALSE");
			} else if (isAdmin(roles)) {
				map.addAttribute("header", "header_admin");
				map.addAttribute("footer", "footer_admin");
				map.addAttribute("ok", "TRUE");
			}
		} else {
			map.addAttribute("header", "header_login");
			map.addAttribute("footer", "footer_login");
			map.addAttribute("ok", "FALSE");
		}
		
		PagedListHolder<?> pages = null;
		int pagesize = 4;
		List<Book> list = null;
		if (principal == null) {
			list = (List<Book>) listBookService.findAll();
			pages = (PagedListHolder<?>) request.getSession().getAttribute("bookListCC");
		} else {
			Person per = userService.findByUsername(principal.getName());
			pages = (PagedListHolder<?>) request.getSession().getAttribute("bookListCR");
			list = getList(per);
		}

		if (pages == null) {
			pages = new PagedListHolder<>(list);
			pages.setPageSize(pagesize);
		} else {
			final int goToPage = pageNumber - 1;
			if (goToPage <= pages.getPageCount() && goToPage >= 0) {
				pages.setPage(goToPage);
			}
		}
		if (principal == null)
			request.getSession().setAttribute("bookListCC", pages);
		else
			request.getSession().setAttribute("bookListCR", pages);
		int current = pages.getPage() + 1;
		int begin = Math.max(1, current - list.size());
		int end = Math.min(begin + 5, pages.getPageCount());
		int totalPageCount = pages.getPageCount();
		String baseUrl = "/book/page/";

		model.addAttribute("beginIndex", begin);
		model.addAttribute("endIndex", end);
		model.addAttribute("currentIndex", current);
		model.addAttribute("totalPageCount", totalPageCount);
		model.addAttribute("baseUrl", baseUrl);
		model.addAttribute("books", pages);

		return "listbook";
	}

	@PreAuthorize("hasRole('ADMIN') or hasRole('EMPLOYEE')")
	@GetMapping("/book/create")
	public String create(Model model, ModelMap map, Authentication authentication) {
		if (authentication != null) {
			Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
			List<String> roles = new ArrayList<String>();
			for (GrantedAuthority a : authorities) {
				roles.add(a.getAuthority());
			}
			if (isUser(roles)) {
				map.addAttribute("header", "header_user");
				map.addAttribute("footer", "footer_user");
			} else if (isAdmin(roles)) {
				map.addAttribute("header", "header_admin");
				map.addAttribute("footer", "footer_admin");
			}
		}
		
		model.addAttribute("book", new Book());
		
		return "savebook";
	}

	@GetMapping("/book/{id}/edit")
	public String edit(@PathVariable int id, Model model, ModelMap map, Authentication authentication) {
		if (authentication != null) {
			Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
			List<String> roles = new ArrayList<String>();
			for (GrantedAuthority a : authorities) {
				roles.add(a.getAuthority());
			}
			
			if (isAdmin(roles)) {
				map.addAttribute("header", "header_admin");
				map.addAttribute("footer", "footer_admin");
			}
			else if (isUser(roles)){
				Long personId = userService.findByUsername(authentication.getName()).getId();
				Long personIdInBook = listBookService.findOne(id).getPerson().getId();
				if (personIdInBook != personId)
					return "redirect:/";
				map.addAttribute("header", "header_user");
				map.addAttribute("footer", "footer_user");
			} 
		}
		else {
				map.addAttribute("header", "header_login");
				map.addAttribute("footer", "footer_login");
				return "redirect:/";
		}
		model.addAttribute("book", listBookService.findOne(id));
		return "savebook";
	}

	@PostMapping("/book/save")
	public String save(@Valid Book book, BindingResult result, RedirectAttributes redirect, ModelMap map, Principal principal) {
		if (result.hasErrors()) {
			map.addAttribute("header", "header_admin");
			map.addAttribute("footer", "footer_admin");
			return "savebook";
		}
		
		if (book.getPerson() == null)
			book.setPerson(userService.findByUsername(principal.getName()));
		listBookService.save(book);
		redirect.addFlashAttribute("success", "Saved book successfully!");
		return "redirect:/book";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/book/{id}/delete")
	public String delete(@PathVariable int id, RedirectAttributes redirect) {
		listBookService.delete(id);
		redirect.addFlashAttribute("success", "Deleted book successfully!");
		return "redirect:/book";
	}

	@GetMapping("/book/search/{pageNumber}")
	public String search(@RequestParam("s") String s, Authentication authentication, Model model,
			HttpServletRequest request, @PathVariable int pageNumber, ModelMap map, Principal principal) {

		if (authentication != null) {
			Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
			List<String> roles = new ArrayList<String>();
			for (GrantedAuthority a : authorities) {
				roles.add(a.getAuthority());
			}
			if (isUser(roles)) {
				map.addAttribute("header", "header_user");
				map.addAttribute("footer", "footer_user");
				map.addAttribute("ok", "FALSE");
			} else if (isAdmin(roles)) {
				map.addAttribute("header", "header_admin");
				map.addAttribute("footer", "footer_admin");
				map.addAttribute("ok", "TRUE");
			}
		} else {
			map.addAttribute("header", "header_login");
			map.addAttribute("footer", "footer_login");
			map.addAttribute("ok", "FALSE");
		}

		if (s.equals("")) {
			return "redirect:/book";
		}

//		List<Book> list = listBookService.search(s);
//		if (list == null) {
//			return "redirect:/book";
//		}

		PagedListHolder<?> pages = null;
		int pagesize = 4;
		
		List<Book> listBookGet = null;
		if (principal == null) {
			listBookGet = (List<Book>) listBookService.findAll();
		} else {
			Person per = userService.findByUsername(principal.getName());
			listBookGet = getList(per);
		}
		
		List<Book> list = new ArrayList<Book>();

		for (Book a : listBookGet) {
			if (String.valueOf(a.getId()).equalsIgnoreCase(s) || a.getTitle().equalsIgnoreCase(s)
					|| is(a.getDomain(), s) || is(a.getAuthors(), s))
				list.add(a);
		}

		for (Book a : listBookGet) {
			if (error(a.getTitle(), s) || error(a.getDomain(), s) || error(a.getAuthors(), s))
				if (!list.contains(a))
					list.add(a);
		}

		pages = new PagedListHolder<>(list);
		pages.setPageSize(pagesize);

		final int goToPage = pageNumber - 1;
		if (goToPage <= pages.getPageCount() && goToPage >= 0) {
			pages.setPage(goToPage);
		}

		if (principal == null)
			request.getSession().setAttribute("bookListCC", pages);
		else
			request.getSession().setAttribute("bookListCR", pages);
		int current = pages.getPage() + 1;
		int begin = Math.max(1, current - list.size());
		int end = Math.min(begin + 5, pages.getPageCount());
		int totalPageCount = pages.getPageCount();
		String baseUrl = "/book/page/";

		model.addAttribute("beginIndex", begin);
		model.addAttribute("endIndex", end);
		model.addAttribute("currentIndex", current);
		model.addAttribute("totalPageCount", totalPageCount);
		model.addAttribute("baseUrl", baseUrl);
		model.addAttribute("books", pages);

		return "listbook";
	}

	private boolean isUser(List<String> roles) {
		if (roles.contains("ROLE_EMPLOYEE")) {
			return true;
		}
		return false;
	}

	private boolean isAdmin(List<String> roles) {
		if (roles.contains("ROLE_ADMIN")) {
			return true;
		}
		return false;
	}

	boolean is(String a, String b) {
		b.replace("+", " ");
		return b.equalsIgnoreCase(a);
	}
	
	boolean error(String a, String b) {
		String[] arr = b.split("\\+");
		for (String item : arr)
			if (a.contains(item))
				return true;
		return false;
	}
}
