package com.teamtreehouse.blog;

import com.teamtreehouse.blog.dao.BlogDao;
import com.teamtreehouse.blog.dao.BlogEntryDAO;
import com.teamtreehouse.blog.model.NotFoundException;
import com.teamtreehouse.blog.model.SimpleBlogEntryDAO;
import com.teamtreehouse.blog.model.Comment;
import com.teamtreehouse.blog.model.SimpleBlogDAO;
import spark.ModelAndView;
import spark.Request;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.*;

import static spark.Spark.*;

public class Main {
    private static final String flashMessage = "";

    private static void setFlashMessage(Request req, String message) {
        req.session().attribute(flashMessage, message);
    }

    private static String getFlashMessage(Request req) {
        if (req.session(false) == null) {
            return null;
        }
        if (!req.session().attributes().contains(flashMessage)) {
            return null;
        }
        return (String) req.session().attribute(flashMessage);
    }

    private static String captureFlashMessage(Request req) {
        String message = getFlashMessage(req);
        if (message != null) {
            req.session().removeAttribute(flashMessage);
        }
        return message;
    }

    public static void main(String[] args) {
        staticFileLocation("/public");
        BlogDao dao = new SimpleBlogDAO();
        Map<String, Object> model = new HashMap<>();

        BlogEntryDAO entry1 = new SimpleBlogEntryDAO("test1","test","This blog post is a test.");
        BlogEntryDAO entry2 = new SimpleBlogEntryDAO("test2","test","This blog post is also a test.");
        BlogEntryDAO entry3 = new SimpleBlogEntryDAO("test3","test","This blog post is the third test.");
        entry1.addTag("test");
        entry2.addTag("test");
        entry3.addTag("test");
        dao.addEntry(entry1);
        dao.addEntry(entry2);
        dao.addEntry(entry3);

        before((req, res) -> {
            if (req.cookie("username") != null) {
                req.attribute("username", req.cookie("username"));
            }
            model.put("username", req.attribute("username"));
        });

        get("/", (req, res) -> {
            model.put("flashMessage", captureFlashMessage(req));
            return new ModelAndView(model, "/hbs/index.hbs");
        }, new HandlebarsTemplateEngine());

        get("/index", (req, res) -> {
            model.put("flashMessage", captureFlashMessage(req));
            return new ModelAndView(model, "/hbs/index.hbs");
        }, new HandlebarsTemplateEngine());

        get("/entries", (req, res) -> {
            model.remove("tag");
            model.remove("author");
            model.put("flashMessage", captureFlashMessage(req));
            model.put("entries", dao.findAllEntries());
            return new ModelAndView(model, "/hbs/entries.hbs");
        }, new HandlebarsTemplateEngine());

        get("/new", (req, res) -> {
            if(req.attribute("username") != null) {
                model.put("flashMessage", captureFlashMessage(req));
                return new ModelAndView(model, "/hbs/new.hbs");
            } else {
                setFlashMessage(req, "Please log in first");
                res.redirect("/log-in");
                return null;
            }
        }, new HandlebarsTemplateEngine());

        post("/entries", (req, res) -> {
            BlogEntryDAO entry = new SimpleBlogEntryDAO(req.queryParams("title"), req.attribute("username"), req.queryParams("content"));
            Set<String> tags = new TreeSet<>();
            Collections.addAll(tags, req.queryParams("tags").toLowerCase().split("[\\W\\s_]+"));
            tags.forEach(entry::addTag);
            dao.addEntry(entry);
            setFlashMessage(req,"\"" + entry.getTitle() + "\""+" created successfully!");
            res.redirect("/entries");
            return null;
        });

        get("/entries/:slug", (req, res) -> {
            model.put("flashMessage", captureFlashMessage(req));
            model.put("entry", dao.findEntryBySlug(req.params("slug")));
            return new ModelAndView(model, "/hbs/entry.hbs");
        }, new HandlebarsTemplateEngine());

        post("/entries/:slug/comment", (req, res) -> {
            BlogEntryDAO entry = dao.findEntryBySlug(req.params("slug"));
            String author;
            if (req.attribute("username") != null) {
                author = req.attribute("username");
            } else {
                author = "anonymous";
            }
            entry.addComment(new Comment(req.queryParams("title"), author, req.queryParams("comment")));
            setFlashMessage(req, "Comment posted!");
            res.redirect("/entries/" + entry.getSlug());
            return null;
        });

        get("/log-in", (req, res) -> {
            model.put("flashMessage", captureFlashMessage(req));
            return new ModelAndView(model, "/hbs/log-in.hbs");
        }, new HandlebarsTemplateEngine());

        post("/log-in", (req, res) -> {
            String username = req.queryParams("username");
            res.cookie("username", username);
            res.redirect("/entries");
            return null;
        });

        get("/:author", (req, res) -> {
            String author = req.params("author");
            model.remove("tag");
            model.put("flashMessage", captureFlashMessage(req));
            model.put("entries", dao.entriesByAuthor(author));
            model.put("author", author);
            return new ModelAndView(model, "/hbs/entries.hbs");
        }, new HandlebarsTemplateEngine());

        get("/entries/:slug/edit", (req, res) -> {
            BlogEntryDAO entry = dao.findEntryBySlug(req.params("slug"));
            if ((entry.getAuthor()).equals(req.attribute("username")) || (req.attribute("username")).equals("admin")) {
                model.put("entry", entry);
                model.put("flashMessage", captureFlashMessage(req));
                return new ModelAndView(model, "/hbs/edit.hbs");
            } else {
                setFlashMessage(req, "You do not have permission to edit this post.");
                res.redirect("/entries/" + entry.getSlug());
                return null;
            }
        }, new HandlebarsTemplateEngine());

        post("/entries/:slug/edit", (req, res) -> {
            BlogEntryDAO entry = dao.findEntryBySlug(req.params("slug"));
            entry.setTitle(req.queryParams("title"));
            entry.setContent(req.queryParams("content"));
            entry.setDate();
            entry.setSlug();
            Set<String> tags = new TreeSet<>();
            Collections.addAll(tags, req.queryParams("tags").toLowerCase().split("[\\W\\s_]+"));
            tags.forEach(entry::addTag);
            setFlashMessage(req, "Post updated successfully!");
            res.redirect("/entries/" + entry.getSlug());
            return null;
        });

        post("/entries/:slug/delete", (req, res) -> {
            BlogEntryDAO entry = dao.findEntryBySlug(req.params("slug"));
            if ((entry.getAuthor()).equals(req.attribute("username")) || (req.attribute("username")).equals("admin")) {
                dao.deleteEntry(entry);
                setFlashMessage(req, "\"" + entry.getTitle() + "\" deleted successfully.");
                res.redirect("/entries");
            } else {
                setFlashMessage(req, "You do not have permission to delete this post!");
                res.redirect("/entries/" + entry.getSlug());
            }
            return null;
        });

        get("/tags/:tag", (req, res) -> {
            String tag = req.params("tag");
            model.remove("author");
            model.put("flashMessage", captureFlashMessage(req));
            model.put("tag", tag);
            model.put("entries", dao.entriesByTag(tag));
            return new ModelAndView(model, "/hbs/entries.hbs");
        }, new HandlebarsTemplateEngine());

        exception(NotFoundException.class, (exc, req, res) -> {
            res.status(404);
            HandlebarsTemplateEngine engine = new HandlebarsTemplateEngine();
            String html = engine.render(new ModelAndView(null, "/hbs/not-found.hbs"));
            res.body(html);
        });
    }
}

