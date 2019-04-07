# Website Generator

This is a script that will generate the html for our website, and upload it to our hosting account.

You can visit the website at http://team1091.com



## How to post blog entries

Create a new markdown file under src/main/resources/blog/published/YYYY/Article-Title.md

```Markdown
    ---
    title: Your Easily Readable Title
    date: YYYY-MM-DD
    ---
    # Article Title
    Write your content in markdown here.

```

## How to upload top level pages
If you want a top level page, you can put a markdown file under src/main/resources/{url}.md


```Markdown
    ---
    title: This Title Appears in Menus
    ---
    # Page Title
    Write your content in markdown here.

```

# Styling
We use [Bootstrap 4](https://getbootstrap.com/) for a base style of this website.  It's a 




## History
We used to use a Wordpress instance, but it got targeted in an automatic cryptojacking campaign.  
This script is an attempt to do low maintenance blog hosting on our existing php/html hosting provider with 
a minimal attack surface.

Q: There are probably like 100 static generation frameworks, why write your own?  https://www.staticgen.com/

A: They are not hard to write, why do you think there are so many?  Besides, I kinda wanted to write one.
