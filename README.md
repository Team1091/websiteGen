# Website Generator
Website Generator


This is a script that will generate the html for our website, and upload it to our hosting account.  

## History
We used to use a Wordpress instance, but it got targeted in an automatic cryptojacking campaign.  
This script is an attempt to do low maintenance blog hosting on our existing php/html hosting provider with 
a minimal attack surface.

## How to post blog entries

Create a new markdown file under src/main/resources/blog/published/YYYY/YYYY-MM-DD-Article-Title.md

```
---
title: Your Easily Readable Title
---
# Article Title
Write your content in markdown here.

```

## How to upload top level pages
If you want a top level page, you can put a markdown file under src/main/resources/url.md



```
---
title: This Title Appears in Menus
---
# Page Title
Write your content in markdown here.

```



Q: There are probably like 100 static generation frameworks, why write your own?  https://www.staticgen.com/

A: They are not hard to write, why do you think there are so many?  Besides, I kinda wanted to write one.
