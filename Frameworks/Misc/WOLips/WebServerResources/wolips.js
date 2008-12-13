if (typeof Prototype == 'undefined') {
  alert('You do not have prototype.js setup properly for WOLips.framework. Either link to Ajax.framework or set the wolips.prototype.framework and wolips.prototype.fileName properties as described in the README.');
}

var WOLipsClickToOpen = {
	id : null,
	verb : null,
	
  url : $H(),
  
  active : false,
  ignoreClick : true,
  
  oldSelectHandler : null,
  oldClickHandler : null,
  oldMoveHandler : null,
  oldKeyPressHandler : null,
  
  lastTarget : null,
  
  start : function(id, verb) {
  	if (WOLipsClickToOpen.verb != null && WOLipsClickToOpen.verb != verb) {
      WOLipsClickToOpen.stop();
  	}
  	
  	WOLipsClickToOpen.id = id;
  	WOLipsClickToOpen.verb = verb;
    if (!WOLipsClickToOpen.active) {
      WOLipsClickToOpen.hideComponentList();
      $(id).innerHTML = '<span class = "_wolUnimportant">' + verb + '</span> <span id = "_componentBreadCrumb" class = "_wolImportant">&nbsp;</span>';
      WOLipsClickToOpen.oldClickHandler = document.onclick;
      WOLipsClickToOpen.oldMoveHandler = document.onmousemove;
      WOLipsClickToOpen.oldSelectHandler = document.onselectstart;
      WOLipsClickToOpen.oldKeyPressHandler = document.onkeypress;
      document.onmousemove = WOLipsClickToOpen.mouseMoved;
      document.onclick = WOLipsClickToOpen.mouseClicked;
      document.onselectstart = WOLips.denyHandler;
      document.onkeypress = WOLipsClickToOpen.keyPressed;
      WOLipsClickToOpen.ignoreClick = true;
      WOLipsClickToOpen.active = true;
    }
    else {
      WOLipsClickToOpen.stop();
    }
  },
  
  stop : function() {
    $(WOLipsClickToOpen.id).innerHTML = 'Click to ' + WOLipsClickToOpen.verb;
    document.onclick = WOLipsClickToOpen.oldClickHandler;
    document.onmousemove = WOLipsClickToOpen.oldMoveHandler;
    document.onselectstart = WOLipsClickToOpen.oldSelectHandler;
    document.onkeypress = WOLipsClickToOpen.oldKeyPressHandler;
    WOLipsClickToOpen.oldClickHandler = null;
    WOLipsClickToOpen.oldMoveHandler = null;
    WOLipsClickToOpen.oldSelectHandler = null;
    WOLipsClickToOpen.oldKeyPressHandler = null;
    WOLipsClickToOpen.active = false;
    WOLipsClickToOpen.targetChanged(null, false);
  },
  
  targetChanged : function(target, highlight) {
    if (target != WOLipsClickToOpen.lastTarget || !highlight) {
      if (WOLipsClickToOpen.lastTarget != null) {
        WOLipsClickToOpen.lastTarget.removeClassName('_wolSelected');
      }
      if (!highlight) {
        WOLipsClickToOpen.lastTarget = null;
      }
      else {
        WOLipsClickToOpen.lastTarget = target;
        if (target != null) {
          target.addClassName('_wolSelected');
        }
      }
    }
  },
  
  keyPressed : function(e) {
    if (!e) var e = window.event;
    if (e.keyCode == Event.KEY_ESC) {
      WOLipsClickToOpen.stop();
    }
  },
  
  mouseMoved : function(e) {
    if (!e) var e = window.event;
    var target = e.target;
    if (!target) target = e.srcElement;

    var componentStack = WOLipsClickToOpen.componentStackForElement(target);
    if (componentStack != null) {
      //WOLipsClickToOpen.targetChanged(componentStack.firstComponent, e.ctrlKey);
      WOLipsClickToOpen.targetChanged(componentStack.firstComponent, e.metaKey);
      var componentBreadCrumb = [];
      componentStack.componentNames.each(function(value, index) {
        var componentParts = value.split('.');
        componentBreadCrumb.push(componentParts[componentParts.length - 1]);
      });
      var componentBreadCrumbElement = $('_componentBreadCrumb');
      if (componentBreadCrumbElement != null) {
        componentBreadCrumbElement.innerHTML = componentBreadCrumb.join(' <span class = "_wolUnimportant">&lt;</span> ');
      }
    }
  },
  
  mouseClicked : function(e) {
    if (WOLipsClickToOpen.ignoreClick) {
      WOLipsClickToOpen.ignoreClick = false;
      return true;
    }

    if (!e) var e = window.event;
    var target = e.target;
    if (!target) target = e.srcElement;

    var componentStack = WOLipsClickToOpen.componentStackForElement(target);
    if (componentStack == null || componentStack.componentNames.length == 0) {
      alert('The component you selected could not be identifed.  Make sure er.component.clickToOpen=true.');
    }
    else if (WOLipsClickToOpen.url[WOLipsClickToOpen.verb] == null) {
      alert('You do not have a click-to-open url set.');
    }
    else if (componentStack.componentNames.length == 1) {
      WOLipsClickToOpen.openComponentNamed(componentStack.componentNames[0]);
    }
    else {
      //if (e.ctrlKey) {
      if (e.metaKey) {
        WOLipsClickToOpen.showComponentList(componentStack.componentNames, Event.pointerX(e), Event.pointerY(e));
      }
      else {
        WOLipsClickToOpen.openComponentNamed(componentStack.componentNames[0]);
      }
    }
    Event.stop(e);
    WOLipsClickToOpen.stop();
    return false;
  },
  
  openComponentNamed : function(selectedComponentName) {
    WOLips.perform(WOLipsClickToOpen.url[WOLipsClickToOpen.verb].replace('REPLACEME', selectedComponentName));
  },
  
  hideComponentList : function() {
    var componentList = $('_clickToOpenComponentList');
    if (componentList != null) {
      $('_clickToOpenComponentList').remove();
    }
  },
  
  showComponentList : function(componentNames, x, y) {
    componentNames.push(null);
    
    var componentNamesContainer = document.createElement("div");
    componentNamesContainer.id = '_clickToOpenComponentList';

    var componentNamesTitle = document.createElement("h1");
    componentNamesTitle.innerHTML = 'Select a Component';
    componentNamesContainer.appendChild(componentNamesTitle);
    
    var componentNamesList = document.createElement("ul");
    componentNames.each(function(componentName, index) {
      var componentNameItem = document.createElement("li");
      componentNameItem._componentName = componentName;
      if (componentName == null) {
        componentNameItem.innerHTML = 'Cancel';
      }
      else {
        componentNameItem.innerHTML = componentName.split('.').last();
      }
      componentNameItem.onclick = function() {
        if (componentNameItem._componentName != null) {
          WOLipsClickToOpen.openComponentNamed(componentNameItem._componentName);
        }
        WOLipsClickToOpen.hideComponentList();
      };
      componentNamesList.appendChild(componentNameItem);
    });
    componentNamesContainer.style.left = x + 'px';
    componentNamesContainer.style.top = y + 'px';
    componentNamesContainer.appendChild(componentNamesList);
    document.getElementsByTagName("body").item(0).appendChild(componentNamesContainer);
  },
  
  componentStackForElement : function(target) {
    var firstComponentElement = null;
    var componentNamesStr = null;
    while (target != null) {
      if (target.getAttribute) {
        var componentName = target.getAttribute('_componentName');
        if (componentName != null) {
          if (componentNamesStr == null) {
            componentNamesStr = componentName;
          }
          else {
            componentNamesStr += "," + componentName; 
          }
          if (firstComponentElement == null) {
            firstComponentElement = target;
          }
        }
        target = Element.up(target);
      }
      else {
        target = null;
      }
    }
    var componentNames;
    if (componentNamesStr == null) {
      componentNames = null;
    } else {
      componentNames = { firstComponent: firstComponentElement, componentNames: componentNamesStr.split(',') };
    }
    return componentNames;
  }
}

var WOLipsToolBar = {
  initialize : function(e) {
    $('_wolToolBarContainer').onselectstart = WOLips.denyHandler;
    WOLipsToolBar.update();
  },
  
  open : function(e) {
    $('_wolToolBar').show();
    WOLipsToolBar.update();
  },
  
  hide : function(e) {
    $('_wolToolBar').hide();
    WOLipsToolBar.update();
  },
  
  toggle : function(e) {
    $('_wolToolBar').toggle();
    WOLipsToolBar.update();
    if (!e) var e = window.event;
    Event.stop(e);
  },
  
  update : function() {
    if ($('_wolToolBar').visible()) {
      $('_wolHandle').innerHTML = '&lt;';
      $('_wolHandle').onclick = WOLipsToolBar.toggle;
      $('_wolToolBarContainer').onclick = null;
      $('_wolToolBarContainer').style.cursor = 'auto';
    }
    else {
      $('_wolToolBarContainer').style.cursor = 'pointer';
      $('_wolToolBarContainer').onclick = WOLipsToolBar.toggle;
      $('_wolHandle').innerHTML = '&gt;';
      $('_wolHandle').onclick = null;
    }
  }
};

var WOLips = {
  controlFrame : null,

  denyHandler : function() {
    return false;
  },
  
  perform : function(url) {
    if (WOLips.controlFrame == null) {
      WOLips.controlFrame = document.createElement("iframe");
      WOLips.controlFrame.style.display = 'none';
      document.getElementsByTagName("body").item(0).appendChild(WOLips.controlFrame);
    }
    WOLips.controlFrame.src = url;
    //setTimeout(function () { document.getElementsByTagName("body").item(0).removeChild(iframeTag); }, 1000);
  }
};
