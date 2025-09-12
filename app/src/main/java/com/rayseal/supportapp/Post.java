package com.rayseal.supportapp;

import java.util.List;

public class Post {
  public String content;
  public List<String> categories;
  public Post(String content, List<String> categories) {
    this.content = content;
    this.categories = categories;
  }
}
