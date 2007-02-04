//
//  Assignment.m
//  RuleModeler
//
//  Created by King Chung Huang on Thu Jan 29 2004.
//  Copyright (c) 2004 King Chung Huang. All rights reserved.
//

#import "Assignment.h"
#import "Rule.h"
#import "NSPropertyListSerializationAdditions.h"
#import "EOControl.h"


@interface Assignment (Private)
+ (NSMutableDictionary *)toolTipDictionary;
@end

@implementation Assignment

+ (void)initialize {
    // Do not call super - see +initialize documentation
    [self setKeys:[NSArray arrayWithObject:@"value"] triggerChangeNotificationsForDependentKey:@"valueAsString"];
    [self setKeys:[NSArray arrayWithObject:@"value"] triggerChangeNotificationsForDependentKey:@"valueDescription"];
    [self setKeys:[NSArray arrayWithObject:@"value"] triggerChangeNotificationsForDependentKey:@"toolTip"];
    [self setKeys:[NSArray arrayWithObject:@"assignmentClass"] triggerChangeNotificationsForDependentKey:@"assignmentClassDescription"];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(defaultsDidChange:) name:NSUserDefaultsDidChangeNotification object:[NSUserDefaults standardUserDefaults]];
}

- (id)init {
    if (self = [super init]) {
        [[self undoManager] disableUndoRegistration];
        
        [self setKeyPath:nil];
        [self setValue:nil];
        [self setAssignmentClass:@"com.webobjects.directtoweb.Assignment"];
        
        [[self undoManager] enableUndoRegistration];
    }
    
    return self;
}

- (void)dealloc {
    [_value release];
    [_keyPath release];
    [_assignmentClass release];
    [_valueDescription release];
    [_assignmentClassDescription release];
    [super dealloc];
}

- (id)initWithKeyValueUnarchiver:(EOKeyValueUnarchiver *)unarchiver {
    if (self = [super init]) {
	[self setKeyPath: [unarchiver decodeObjectForKey:(@"keyPath")]];
        [self setValue: [unarchiver decodeObjectForKey:(@"value")]];
    }
    
    return self;
}

- (void)encodeWithKeyValueArchiver:(EOKeyValueArchiver *)archiver {
    if(_keyPath == nil)
        [archiver encodeObject:@"" forKey:@"keyPath"]; // RuleEditor does it like this; if there is no keyPath, D2W runtime will throw an exception
    else
        [archiver encodeObject:_keyPath forKey:@"keyPath"];
    if(_value != nil) // RuleEditor does it like this
        [archiver encodeObject:_value forKey:@"value"];
    [archiver encodeObject:(_assignmentClass ? _assignmentClass : @"") forKey:@"class"]; // Let's always provide an empty class name as we cannot remove the class key from the archive, else user would see 'Assignment' instead of nothing in the preview and the (string) copy
}

- (NSString *)description {
    return [NSString stringWithFormat:@"(%@) %@ = %@", [self assignmentClass], [self keyPath], [self valueDescription]];
}

- (NSString *)assignmentClass {
    return _assignmentClass;
}

- (void)setAssignmentClass:(NSString *)assignmentClass {
	if(![_assignmentClass isEqualToString:assignmentClass]) {
        [[[self undoManager] prepareWithInvocationTarget:self] setAssignmentClass:_assignmentClass];
        [self _setActionName:NSLocalizedString(@"Set RHS Class to %@", @"Undo-redo action name") old:_assignmentClass new:assignmentClass];
        
        [_assignmentClass release];
        _assignmentClass = [assignmentClass copyWithZone:[self zone]];
        [_assignmentClassDescription release];
        _assignmentClassDescription = nil;
    }
}

static NSMutableCharacterSet  *fullyQualifiedClassNameCharSet = nil;
- (BOOL)validateAssignmentClassDescription:(id *)ioValue error:(NSError **)outError {
    if ([*ioValue length] > 0) {
        NSScanner   *aScanner = [NSScanner scannerWithString:*ioValue];
        NSString    *aClassName = nil;
        NSString    *aPackageName = nil;
        
        if (!fullyQualifiedClassNameCharSet) {
            fullyQualifiedClassNameCharSet = [[NSMutableCharacterSet alphanumericCharacterSet] retain];
            [fullyQualifiedClassNameCharSet addCharactersInString:@"."];
        }        
        
        // MY.PACKAGE.CLASSNAME
        if (![aScanner scanUpToString:@"(" intoString:&aClassName] || [aScanner isAtEnd]) {
            [aScanner setScanLocation:0];
            if ([aScanner scanCharactersFromSet:fullyQualifiedClassNameCharSet intoString:&aClassName]) {
                unsigned    lastDotIndex = [aClassName rangeOfString:@"." options:NSBackwardsSearch].location;
                
                if (lastDotIndex != NSNotFound) {
                    if (lastDotIndex != 0 && lastDotIndex != ([aClassName length] - 1)) {
                        aPackageName = [aClassName substringToIndex:lastDotIndex];
                        aClassName = [aClassName substringFromIndex:lastDotIndex + 1];
                    } else {
                        aPackageName = nil;
                        aClassName = nil;
                    }
                }
            }
        } else { // CLASSNAME (MY.PACKAGE)
            if ([aScanner scanString:@"(" intoString:NULL]) {                
                if (![aScanner scanUpToString:@")" intoString:&aPackageName]) {
                    [aScanner scanCharactersFromSet:fullyQualifiedClassNameCharSet intoString:&aPackageName];
                }
            }
        }
        
        if (!aClassName) {
            if (outError) {
                *outError = [NSError errorWithDomain:@"RuleModeler" code:0 userInfo:[NSDictionary dictionaryWithObject:NSLocalizedString(@"This is not a valid java class name", @"Validation error description") forKey:NSLocalizedDescriptionKey]];
            }
            return NO;
        } else {
            if (aPackageName) {
                *ioValue = [NSString stringWithFormat:@"%@ (%@)", aClassName, aPackageName];
            } else {
                *ioValue = aClassName;
            }
        }
    }
    
    return YES;
}

- (NSString *)assignmentClassDescription {
    if (_assignmentClassDescription == nil && _assignmentClass != nil) {
        _assignmentClassDescription = _assignmentClass;
        if (![self validateAssignmentClassDescription:&_assignmentClassDescription error:NULL]) {
            _assignmentClassDescription = [_assignmentClass retain];
        } else {        
            [_assignmentClassDescription retain];
        }
    }
    
    return _assignmentClassDescription;
}

- (void)setAssignmentClassDescription:(NSString *)classnameDescription {
    // Always expects formatted string: "CLASS (PACKAGE)" or "CLASS" (when no package)
    NSString    *aString = nil;
    
    if (classnameDescription) {
        NSRange     aRange = [classnameDescription rangeOfString:@"("];
        
        if (aRange.length > 0) {
            aString = [NSString stringWithFormat:@"%@.%@", [classnameDescription substringWithRange:NSMakeRange(aRange.location + 1, [classnameDescription length] - (aRange.location + 1) - 1)], [classnameDescription substringWithRange:NSMakeRange(0, aRange.location - 1)]];
        } else {
            aString = classnameDescription;
        }
    }
    [self setAssignmentClass:aString];
}

- (NSString *)keyPath {
    return _keyPath;
}

- (void)setKeyPath:(NSString *)keyPath {
    // TODO If empty string, consider as nil -> validateKeyPath
    if(![_keyPath isEqualToString:keyPath]) {
        [[[self undoManager] prepareWithInvocationTarget:self] setKeyPath:_keyPath];
        [self _setActionName:NSLocalizedString(@"Set RHS Key to %@", @"Undo-redo action name") old:_keyPath new:keyPath];
        
        [_keyPath release];
        _keyPath = [keyPath copyWithZone:[self zone]];
    }
}

- (id)value {
    return _value;
}

- (void)setValue:(id)value {
    if(![_value isEqual:value]){
        [[[self undoManager] prepareWithInvocationTarget:self] setValue:_value];
        [self _setActionName:NSLocalizedString(@"Set RHS Value to %@", @"Undo-redo action name") old:_value new:value];
        
        [_value autorelease];
        
        _value = [value copyWithZone:[self zone]];
        [_valueDescription release];
        _valueDescription = nil;
    }
}

- (BOOL)validateValueAsString:(id *)ioValue error:(NSError **)outError prettyPrint:(BOOL)prettyPrint {
    BOOL    isValid = YES;
    
    NS_DURING {
        // In order to support not quoted string values (like RuleEditor does),
        // we enclose value with "" when first char is not a ( nor a { nor a \",
        // nor value is the empty string. We also trim spaces and newlines.
        *ioValue = [*ioValue stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
        if ([*ioValue length] > 0 && ![*ioValue hasPrefix:@"("] && ![*ioValue hasPrefix:@"{"] && ![*ioValue hasPrefix:@"\""]) {
            *ioValue = [[@"\"" stringByAppendingString:*ioValue] stringByAppendingString:@"\""];
        }
        
        if([NSPropertyListSerialization propertyList:*ioValue isValidForFormat:NSPropertyListOpenStepFormat]) {
            // The first check isn't enough: it might return YES, but conversion to plist would fail!
            NSString *string = *ioValue;
            NSData *data = [string dataUsingEncoding:NSUTF8StringEncoding];
            NSString *errorString = nil;
            id plist = [NSPropertyListSerialization propertyListFromData:data mutabilityOption:NSPropertyListImmutable format:NULL errorDescription:&errorString];
            if(errorString) {
                isValid = NO;
                *outError = [NSError errorWithDomain:@"RuleModeler" code:0 userInfo:[NSDictionary dictionaryWithObject:[NSString stringWithFormat:NSLocalizedString(@"This is not a valid right value (property list):\n\n%@", @"Validation error description"), errorString] forKey:NSLocalizedDescriptionKey]];
            } else {
                // Reformat string (normalize)
                *ioValue = [NSPropertyListSerialization openStepFormatStringFromPropertyList:plist prettyPrint:prettyPrint escapeNonASCII:NO errorDescription:&errorString];
                NSAssert1(errorString == nil, @"Unable to get OpenStep format string from property list rhs value: %@", errorString);
            }
        } else {
            *outError = [NSError errorWithDomain:@"RuleModeler" code:0 userInfo:[NSDictionary dictionaryWithObject:NSLocalizedString(@"This is not a valid right value (property list).", @"Validation error description") forKey:NSLocalizedDescriptionKey]];
        }
    } NS_HANDLER {
        NSLog(@"%s: Error coding: %@: %@", __PRETTY_FUNCTION__, [*ioValue className], *ioValue);
        isValid = NO;
        *outError = [NSError errorWithDomain:@"RuleModeler" code:0 userInfo:[NSDictionary dictionaryWithObject:NSLocalizedString(@"This is not a valid right value (property list).", @"Validation error description") forKey:NSLocalizedDescriptionKey]];
    } NS_ENDHANDLER;    
        
    return isValid;
}

- (BOOL)validateValueAsString:(id *)ioValue error:(NSError **)outError {
    return [self validateValueAsString:ioValue error:outError prettyPrint:YES];
}

- (NSString *)valueAsString {
    // Multiple-line description - used for edition is detail view
    if(_value != nil){
        NSString    *errorString = nil;
        NSString    *aString = [NSPropertyListSerialization openStepFormatStringFromPropertyList:_value level:1 escapeNonASCII:NO errorDescription:&errorString];
        
        NSAssert1(errorString == nil, @"Unable to get OpenStep format string from property list rhs value: %@", errorString);

        return aString;
    }
    else
        return nil;
}

- (void)setValueFromString:(NSString *)value {
    id plist;
    
    if (value) {
        NSData *data = [value dataUsingEncoding:NSUTF8StringEncoding];
        NSString *errorString = nil;
        plist = [NSPropertyListSerialization propertyListFromData:data mutabilityOption:NSPropertyListImmutable format:NULL errorDescription:&errorString];
        NSAssert1(errorString == nil, @"Unable to get OpenStep format string from property list rhs value: %@", errorString);
    }
    else
        plist = nil;
    [self setValue:plist];
}

- (void)setValueAsString:(NSString *)value {
    [self setValueFromString:value];
}

- (BOOL)validateValueDescription:(id *)ioValue error:(NSError **)outError {
    return [self validateValueAsString:ioValue error:outError prettyPrint:NO];
}

- (NSString *)valueDescription {
    // One-line description - used for search and table cell edition
    if(_valueDescription == nil && _value != nil){
        // NSPredicate doesn't support line returns!
        NSString    *errorString = nil;
        
        _valueDescription = [[NSPropertyListSerialization openStepFormatStringFromPropertyList:_value prettyPrint:NO escapeNonASCII:NO errorDescription:&errorString] retain];
        NSAssert1(errorString == nil, @"Unable to get OpenStep format string from property list rhs value: %@", errorString);
	}
    
	return _valueDescription;
}

- (void)setValueDescription:(NSString *)value {
    [self setValueFromString:value];
}

// Undo management
- (NSUndoManager *)undoManager {
	return [_rule undoManager];
}

- (void)_setActionName:(NSString *)format old:(id)oldValue new:(id)newValue {
    NSUndoManager *um = [self undoManager];
    
    if ([um isUndoing]) {
        [um setActionName:[NSString stringWithFormat:format, oldValue]];
    } else {
        [um setActionName:[NSString stringWithFormat:format, newValue]];
    }
}

- (void)setRule:(Rule *)rule {
	_rule = rule; // Back-pointer - not retained
}

+ (void)refreshToolTipDictionary {
    NSArray         *paths = [[NSUserDefaults standardUserDefaults] arrayForKey:@"d2wclientConfigurationPaths"];
    NSEnumerator    *anEnum = [paths objectEnumerator];
    NSString        *aPath;
    
    [[self toolTipDictionary] removeAllObjects];
    while(aPath = [anEnum nextObject]){
        NSDictionary    *aDict = [NSDictionary dictionaryWithContentsOfFile:[aPath stringByStandardizingPath]];
        
        if(aDict){
            aDict = [aDict objectForKey:@"components"];
            if(aDict)
                [[self toolTipDictionary] addEntriesFromDictionary:aDict];
        }
    }
}

+ (NSMutableDictionary *)toolTipDictionary {
    static NSMutableDictionary *aDict = nil;
    
    if(aDict == nil){
        aDict = [[NSMutableDictionary alloc] init];
        [self refreshToolTipDictionary];
    }
    
    return aDict;
}

+ (void)defaultsDidChange:(NSNotification *)notif {
    [self refreshToolTipDictionary];
}

- (NSString *)toolTip {
    if([_value isKindOfClass:[NSString class]] && [_value length] > 0)
        return [[[self class] toolTipDictionary] valueForKeyPath:[_value stringByAppendingString:@".inspectionInformation"]];
    else
        return nil;
}

- (BOOL)isEqualToAssignment:(Assignment *)assignment {
    NSParameterAssert ([assignment isKindOfClass:[Assignment class]]);
    return (((![self keyPath] && ![assignment keyPath]) || [[self keyPath] isEqualToString:[assignment keyPath]])
            && ((![self value] && ![assignment value]) || [[self value] isEqual:[assignment value]])
            && ((![self assignmentClass] && ![assignment assignmentClass]) || [[self assignmentClass] isEqualToString:[assignment assignmentClass]]));
}

@end
