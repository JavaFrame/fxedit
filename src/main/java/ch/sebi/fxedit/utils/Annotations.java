package ch.sebi.fxedit.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A class with handy functions for using annotations
 * @author sebi
 *
 */
public class Annotations {
	/**
	 * Finds the annotation which are defined on the search class (NOT the implemented interfaces or super class)
	 * @param searchClass the class to search in
	 * @param annotationClass the annotation class
	 * @return all found annotations
	 */
	public static <T extends Annotation> Optional<AnnotationMatch<T>> findAnnotation(Class<?> searchClass, Class<T> annotationClass) {
		List<AnnotationMatch<T>> l = findAnnotations(searchClass, annotationClass);
		if(l.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(l.get(0));
	}
	/**
	 * Finds the annotations which are defined on the search class (NOT the implemented interfaces or super class)
	 * @param searchClass the class to search in
	 * @param annotationClass the annotation class
	 * @return all found annotations
	 */
	public static <T extends Annotation> List<AnnotationMatch<T>> findAnnotations(Class<?> searchClass, Class<T> annotationClass) {
		List<AnnotationMatch<T>> matches = new ArrayList<>();
		Field[] fields = searchClass.getDeclaredFields();
		Constructor<?>[] constructors = searchClass.getDeclaredConstructors();
		Method[] methods = searchClass.getDeclaredMethods();
		//adds annotations on the class itself
		matches.addAll(getAnnotations(annotationClass, new AnnotatedElement[] { searchClass }));
		//adds annotations on the constructor
		matches.addAll(getAnnotations(annotationClass, constructors));
		//adds annotations on the declared fields
		matches.addAll(getAnnotations(annotationClass, fields));
		//adds annotations on the declared methods
		matches.addAll(getAnnotations(annotationClass, methods));
		return matches;
	}

	/**
	 * Finds the annotations which are defined on the search class, the implemented interfaces or the super class.
	 * It will search through the whole class hierarchy of the search class
	 * @param searchClass the class to search in
	 * @param annotationClass the annotation class
	 * @return all found annotations
	 */
	public static <T extends Annotation> List<AnnotationMatch<T>> findAnnotationsInHierarchy(Class<?> searchClass, Class<T> annotationClass) {
		List<AnnotationMatch<T>> matches = new ArrayList<>();
		matches.addAll(findAnnotations(searchClass, annotationClass));
		Class<?> superClass = searchClass.getSuperclass();
		//searches in the superclass and adds the matched annotations
		if(superClass != null) {
			matches.addAll(findAnnotationsInHierarchy(superClass, annotationClass));
		}
		//searches in the implemented interfaces and adds the matched annotations
		for(Class<?> parent : searchClass.getInterfaces()) {
			matches.addAll(findAnnotationsInHierarchy(parent, annotationClass));
		}
		return  matches;
	}

	/**
	 * Returns the annotations found on the objs array in an List
	 * @param annotationClass the annotations class
	 * @param objs the AccessibleObjects to search through
	 * @return the list
	 */
	private static <T extends Annotation> List<AnnotationMatch<T>> getAnnotations(Class<T> annotationClass, AnnotatedElement[] objs) {
		List<AnnotationMatch<T>> list = new ArrayList<>();
		for(AnnotatedElement obj : objs) {
			T[] annotations = obj.getAnnotationsByType(annotationClass);
			for(T annotation : annotations) {
				list.add(new AnnotationMatch<T>(obj, annotation));
			}
		}
		return list;
	}
	
	/**
	 * A Annotation match 
	 * @author sebi
	 *
	 * @param <T> the type of the annotation
	 */
	public static class AnnotationMatch<T extends Annotation> {
		/**
		 * the object on which the annotation was found on
		 */
		private AnnotatedElement foundOn;
		/**
		 * the annotation
		 */
		private T annotation;

		/**
		 * constructor
		 * @param foundOn the object on which the annotation was found
		 * @param annotation the annotation
		 */
		public AnnotationMatch(AnnotatedElement foundOn, T annotation) {
			this.foundOn = foundOn;
			this.annotation = annotation;
		}

		/**
		 * Returns the object on which the annotation was found.
		 * @return the object
		 */
		public AnnotatedElement getFoundOn() {
			return foundOn;
		}

		/**
		 * Returns the found annotation
		 * @return the annotation
		 */
		public T getAnnotation() {
			return annotation;
		}
	}
}
